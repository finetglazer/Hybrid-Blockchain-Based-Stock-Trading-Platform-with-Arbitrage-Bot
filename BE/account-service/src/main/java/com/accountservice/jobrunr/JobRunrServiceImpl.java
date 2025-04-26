package com.accountservice.jobrunr;

import com.accountservice.model.BalanceHistory;
import com.accountservice.model.TradingAccount;
import com.accountservice.model.Transaction;
import com.accountservice.repository.BalanceHistoryRepository;
import com.accountservice.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.BackgroundJob;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.Date;
import java.util.List;


@Service
@AllArgsConstructor
@Transactional
public class JobRunrServiceImpl {
    private final MongoTemplate mongoTemplate;

    private final BalanceHistoryRepository balanceHistoryRepository;

    private static final int CHUNK_SIZE = 100;

    @Job(name = "Update balance history", retries = 2)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void updateBalanceHistoryJob() {
        LocalDate previousDay = LocalDate.now().minusDays(1);
        LocalDateTime startTime = LocalDateTime.of(previousDay, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(previousDay, LocalTime.MAX);

        List<TradingAccount> tradingAccounts = mongoTemplate.find(new Query(), TradingAccount.class);
        for (TradingAccount tradingAccount : tradingAccounts) {
            // Asynchronous operation in background for each account
            BackgroundJob.enqueue(() -> {
                this.processTradingAccount(tradingAccount, startTime, endTime);
            });
        }
    }

    @Job(name = "Clear old transactions", retries = 2)
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Ho_Chi_Minh")
    public void clearOldTransactionsJob() {
        // Asynchronous operation in background
        BackgroundJob.enqueue(this::processClearOldTransactions);
    }

    public void processClearOldTransactions() {
        LocalDate sevenDaysBefore = LocalDate.now().minusDays(7);
        LocalDateTime startTime = LocalDateTime.of(sevenDaysBefore, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(sevenDaysBefore, LocalTime.MAX);

        Criteria timeCriteria = Criteria.where("completedAt").gte(startTime).lte(endTime);
        mongoTemplate.remove(new Query(timeCriteria), Transaction.class);
    }

    public void processTradingAccount(TradingAccount tradingAccount, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Date startTime = DateUtils.convertLocalDateTimeToDate(startDateTime);
        Date endTime = DateUtils.convertLocalDateTimeToDate(endDateTime);

        Criteria accountCriteria = Criteria.where("accountId").is(tradingAccount.getId());
        Criteria timeCriteria = Criteria.where("completedAt").gte(startTime).lte(endTime);
        Criteria statusCriteria = Criteria.where("status").is(Transaction.TransactionStatus.COMPLETED.name());
        long transactionCount = mongoTemplate.count(new Query(timeCriteria).addCriteria(accountCriteria).addCriteria(statusCriteria), Transaction.class);
        int pageCount = (int) Math.ceil((double) transactionCount / CHUNK_SIZE);

        for (int page = 0; page < pageCount; ++page) {
            List<Transaction> transactions = mongoTemplate.find(
                    new Query(timeCriteria).addCriteria(accountCriteria).addCriteria(statusCriteria)
                            .skip((long) page * CHUNK_SIZE)
                            .limit(CHUNK_SIZE),
                    Transaction.class
            );
            updateBalanceHistoryJobProcessor(tradingAccount.getId(), tradingAccount.getUserId(), transactions);
        }
    }

    public void updateBalanceHistoryJobProcessor(String accountId, String userId, List<Transaction> transactions) {
        Date yesterday = DateUtils.getDate(1);
        BalanceHistory yesterdayBalanceHistory = balanceHistoryRepository.findBalanceHistoryByAccountIdAndDate(accountId, yesterday);
        assert yesterdayBalanceHistory != null;

        BigDecimal closingBalance = yesterdayBalanceHistory.getOpeningBalance();

        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            String type = transaction.getType();
            if (type.equals(Transaction.TransactionType.DEPOSIT.name())) {
                deposits = deposits.add(transaction.getAmount());
                closingBalance = closingBalance.add(transaction.getAmount());
            }
            else if (type.equals(Transaction.TransactionType.WITHDRAWAL.name())) {
                withdrawals = withdrawals.add(transaction.getAmount());
                closingBalance = closingBalance.subtract(transaction.getAmount());
            }
            else if (type.equals(Transaction.TransactionType.FEE.name())) {
                fees = fees.add(transaction.getAmount());
                closingBalance = closingBalance.subtract(transaction.getAmount());
            }
            else if (type.equals(Transaction.TransactionType.INTEREST.name())) {
                closingBalance = closingBalance.add(transaction.getAmount());
            }
            else if (type.equals(Transaction.TransactionType.TRANSFER.name())) {
                closingBalance = closingBalance.subtract(transaction.getAmount());  // Receive or send ?
            }
            else if (type.equals(Transaction.TransactionType.REFUND.name())) {
                closingBalance = closingBalance.add(transaction.getAmount());
            }
            else {
                closingBalance = closingBalance.subtract(transaction.getAmount());
            }
        }

        yesterdayBalanceHistory.setClosingBalance(closingBalance);
        yesterdayBalanceHistory.setDeposits(deposits);
        yesterdayBalanceHistory.setWithdrawals(withdrawals);
        yesterdayBalanceHistory.setFees(fees);
        yesterdayBalanceHistory.setTradesNet(closingBalance.subtract(yesterdayBalanceHistory.getOpeningBalance()));

        balanceHistoryRepository.save(yesterdayBalanceHistory);

        BalanceHistory nextDayBalanceHistory = new BalanceHistory();
        nextDayBalanceHistory.setDate(DateUtils.getDate(0));
        nextDayBalanceHistory.setAccountId(accountId);
        nextDayBalanceHistory.setUserId(userId);

        nextDayBalanceHistory.setOpeningBalance(closingBalance);
        nextDayBalanceHistory.setClosingBalance(BigDecimal.ZERO);
        nextDayBalanceHistory.setDeposits(BigDecimal.ZERO);
        nextDayBalanceHistory.setWithdrawals(BigDecimal.ZERO);
        nextDayBalanceHistory.setTradesNet(BigDecimal.ZERO);
        nextDayBalanceHistory.setFees(BigDecimal.ZERO);

        balanceHistoryRepository.save(nextDayBalanceHistory);
    }
}
