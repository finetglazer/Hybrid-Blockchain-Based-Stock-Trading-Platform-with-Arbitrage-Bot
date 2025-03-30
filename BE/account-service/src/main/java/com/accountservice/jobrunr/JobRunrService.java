//package com.accountservice.jobrunr;
//
//import com.accountservice.model.BalanceHistory;
//import com.accountservice.model.TradingAccount;
//import com.accountservice.model.Transaction;
//import com.accountservice.repository.BalanceHistoryRepository;
//import com.accountservice.service.TransactionService;
//import lombok.AllArgsConstructor;
//import org.jobrunr.jobs.annotations.Job;
//import org.jobrunr.scheduling.BackgroundJob;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//
//
//@Service
//@AllArgsConstructor
//@Transactional
//public class JobRunrService {
//    private final MongoTemplate mongoTemplate;
//
//    private final BalanceHistoryRepository balanceHistoryRepository;
//
//    private static final int CHUNK_SIZE = 100;
//
//    @Job(name = "Update balance history", retries = 2)
//    @Scheduled(cron = "0 0 0 * * ?")                // 0:00 every day
//    public void updateBalanceHistoryJob() {
//        LocalDate previousDay = LocalDate.now().minusDays(1);
//        LocalDateTime startTime = LocalDateTime.of(previousDay, LocalTime.MIN);
//        LocalDateTime endTime = LocalDateTime.of(previousDay, LocalTime.MAX);
//
//        List<TradingAccount> tradingAccounts = mongoTemplate.find(new Query(), TradingAccount.class);
//        for (TradingAccount tradingAccount : tradingAccounts) {
//            BackgroundJob.enqueue(() -> {           // Asynchronous operation in background for each account
//                Criteria accountCriteria = Criteria.where("accountId").is(tradingAccount.getId());
//                Criteria timeCriteria = Criteria.where("completedAt").gte(startTime).lte(endTime);
//                Criteria statusCriteria = Criteria.where("status").is(Transaction.TransactionStatus.COMPLETED.name());
//                long transactionCount = mongoTemplate.count(new Query(timeCriteria).addCriteria(accountCriteria).addCriteria(statusCriteria), Transaction.class);
//                int pageCount = (int) Math.ceil((double) transactionCount / CHUNK_SIZE);
//
//                for (int page = 0; page < pageCount; ++page) {
//                    List<Transaction> transactions = mongoTemplate.find(
//                            new Query(timeCriteria).addCriteria(accountCriteria).addCriteria(statusCriteria)
//                                    .skip((long) page * CHUNK_SIZE)
//                                    .limit(CHUNK_SIZE),
//                            Transaction.class
//                    );
//                    updateBalanceHistoryJobProcessor(tradingAccount.getId(), tradingAccount.getUserId(), transactions);
//                }
//            });
//        }
//    }
//
//    @Job(name = "Clear old transactions", retries = 2)
//    @Scheduled(cron = "0 0 7 * * ?")                // 7:00 every day
//    public void clearTransactionsOfPreviousDayJob() {
//        BackgroundJob.enqueue(() -> {               // Asynchronous operation in background
//            LocalDate sevenDaysBefore = LocalDate.now().minusDays(7);
//            LocalDateTime startTime = LocalDateTime.of(sevenDaysBefore, LocalTime.MIN);
//            LocalDateTime endTime = LocalDateTime.of(sevenDaysBefore, LocalTime.MAX);
//
//            Criteria timeCriteria = Criteria.where("completedAt").gte(startTime).lte(endTime);
//            mongoTemplate.remove(new Query(timeCriteria), Transaction.class);
//        });
//    }
//
//    private void updateBalanceHistoryJobProcessor(String accountId, String userId, List<Transaction> transactions) {
//        BalanceHistory previousDayBalanceHistory = balanceHistoryRepository.findBalanceHistoryByAccountIdAndDate(accountId, LocalDate.now().minusDays(1));
//        float closingBalance = previousDayBalanceHistory.getOpeningBalance();
//        float deposits = 0;
//        float withdrawals = 0;
//        float fees = 0;
//
//        for (Transaction transaction : transactions) {
//            String type = transaction.getType();
//            if (type.equals(Transaction.TransactionType.DEPOSIT.name())) {
//                deposits += transaction.getAmount();
//                closingBalance += transaction.getAmount();
//            }
//            else if (type.equals(Transaction.TransactionType.WITHDRAWAL.name())) {
//                withdrawals += transaction.getAmount();
//                closingBalance -= transaction.getAmount();
//            }
//            else if (type.equals(Transaction.TransactionType.FEE.name())) {
//                fees += transaction.getAmount();
//                closingBalance -= transaction.getAmount();
//            }
//            else if (type.equals(Transaction.TransactionType.INTEREST.name())) {
//                closingBalance += transaction.getAmount();
//            }
//            else if (type.equals(Transaction.TransactionType.TRANSFER.name())) {
//                closingBalance -= transaction.getAmount();  // Receive or send ?
//            }
//            else if (type.equals(Transaction.TransactionType.REFUND.name())) {
//                closingBalance += transaction.getAmount();
//            }
//            else {
//                closingBalance -= transaction.getAmount();
//            }
//        }
//
//        previousDayBalanceHistory.setClosingBalance(closingBalance);
//        previousDayBalanceHistory.setDeposits(deposits);
//        previousDayBalanceHistory.setWithdrawals(withdrawals);
//        previousDayBalanceHistory.setFees(fees);
//        previousDayBalanceHistory.setTradesNet(closingBalance - previousDayBalanceHistory.getOpeningBalance());
//
//        balanceHistoryRepository.save(previousDayBalanceHistory);
//
//        BalanceHistory nextDayBalanceHistory = new BalanceHistory();
//        nextDayBalanceHistory.setDate(LocalDate.now());
//        nextDayBalanceHistory.setAccountId(accountId);
//        nextDayBalanceHistory.setUserId(userId);
//        nextDayBalanceHistory.setOpeningBalance(closingBalance);
//
//        balanceHistoryRepository.save(nextDayBalanceHistory);
//    }
//}
