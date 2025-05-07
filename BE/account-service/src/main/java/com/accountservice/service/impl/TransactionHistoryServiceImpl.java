package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.common.Const;
import com.accountservice.common.PagingResponse;
import com.accountservice.model.PaymentMethod;
import com.accountservice.model.TradingAccount;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.payload.response.GetTransactionDetailsAccountInfo;
import com.accountservice.payload.response.client.GetTransactionDetailsPaymentMethodInfo;
import com.accountservice.payload.response.client.GetTransactionDetailsResponse;
import com.accountservice.payload.response.internal.GetTransactionsResponse;
import com.accountservice.repository.PaymentMethodRepository;
import com.accountservice.repository.TradingAccountRepository;
import com.accountservice.repository.TransactionRepository;
import com.accountservice.service.TransactionHistoryService;
import com.accountservice.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class TransactionHistoryServiceImpl implements TransactionHistoryService {
    private final MongoTemplate mongoTemplate;

    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TradingAccountRepository tradingAccountRepository;

    @Override
    public BaseResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest getTransactionsRequest) {
        String userId = getTransactionsRequest.getUserId();
        List<String> accountIds = getTransactionsRequest.getAccountIds() == null ? List.of() : getTransactionsRequest.getAccountIds();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDate startDate = LocalDate.parse(getTransactionsRequest.getStartDate() == null ? "1970-01-01" : getTransactionsRequest.getStartDate(), dateFormatter);
        LocalDate endDate = LocalDate.parse(getTransactionsRequest.getEndDate() == null ? "9999-12-31" : getTransactionsRequest.getEndDate(), dateFormatter);
        LocalTime startTime = LocalTime.parse(getTransactionsRequest.getStartTime() == null ? "00:00:00" : getTransactionsRequest.getStartTime(), timeFormatter);
        LocalTime endTime = LocalTime.parse(getTransactionsRequest.getStartTime() == null ? "00:00:00" : getTransactionsRequest.getEndTime(), timeFormatter);
        LocalDateTime startLocalDateTime = startDate.atTime(startTime);
        LocalDateTime endLocalDateTime = endDate.atTime(endTime);
        Date startDateTime = DateUtils.convertLocalDateTimeToDate(startLocalDateTime);
        Date endDateTime = DateUtils.convertLocalDateTimeToDate(endLocalDateTime);
        List<String> statuses = getTransactionsRequest.getStatuses() == null ? List.of() : getTransactionsRequest.getStatuses();
        List<String> types = getTransactionsRequest.getTypes() == null ? List.of() : getTransactionsRequest.getTypes();
        List<String> paymentMethodIds = getTransactionsRequest.getPaymentMethodIds() == null ? List.of() : getTransactionsRequest.getPaymentMethodIds();
        int page = getTransactionsRequest.getPage() == null ? 0 : getTransactionsRequest.getPage();
        int size = getTransactionsRequest.getSize() == null ? 10000 : getTransactionsRequest.getSize();

        Query query = new Query(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime));

        if (!statuses.isEmpty()) {
            query.addCriteria(Criteria.where("status").in(statuses));
        }

        if (!types.isEmpty()) {
            query.addCriteria(Criteria.where("type").in(types));
        }

        if (!accountIds.isEmpty()) {
            query.addCriteria(Criteria.where("accountId").in(accountIds));
        }
        else {
            List<TradingAccount> tradingAccounts = mongoTemplate.find(new Query(Criteria.where("userId").is(userId)), TradingAccount.class);
            query.addCriteria(Criteria.where("accountId").in(tradingAccounts.stream().map(TradingAccount::getId).toList()));
        }

        if (!paymentMethodIds.isEmpty()) {
            query.addCriteria(Criteria.where("paymentMethodId").in(paymentMethodIds));
        }

        long totalTransactions = mongoTemplate.find(query, Transaction.class).size();
        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Transactions retrieved successfully",
            new GetTransactionsResponse(transactions, new PagingResponse(page, size, totalTransactions, (int) Math.ceil((double) totalTransactions / transactions.size())))
        );
    }

    public Transaction getLastTransaction(String accountId) {
        Query query = new Query(Criteria.where("accountId").is(accountId));
        query.with(Sort.by(Sort.Order.desc("createdAt")));
        query.limit(1);

        return mongoTemplate.findOne(query, Transaction.class);
    }

    @Override
    public BaseResponse<?> getTransactionDetails(String transactionId) {
        Transaction transaction = transactionRepository.findTransactionById(transactionId).orElse(null);
        if (transaction == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Transaction not found with transactionId: " + transactionId,
                ""
            );
        }
        PaymentMethod paymentMethod = paymentMethodRepository.findPaymentMethodById(transaction.getPaymentMethodId()).orElse(null);
        if (paymentMethod == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Payment method not found with transactionId: " + transactionId,
                ""
            );
        }
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountById(transaction.getAccountId()).orElse(null);
        if (tradingAccount == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Account not found with transactionId: " + transactionId,
                ""
            );
        }
        GetTransactionDetailsAccountInfo accountInfo = new GetTransactionDetailsAccountInfo();
        accountInfo.setId(transaction.getAccountId());
        accountInfo.setNickname(tradingAccount.getNickname());
        accountInfo.setAccountNumber(tradingAccount.getAccountNumber());
        accountInfo.setAccountNumber(tradingAccount.getAccountNumber());

        GetTransactionDetailsPaymentMethodInfo paymentMethodInfo = new GetTransactionDetailsPaymentMethodInfo();
        paymentMethodInfo.setMaskedNumber(paymentMethod.getMaskedNumber());
        paymentMethodInfo.setId(transaction.getPaymentMethodId());
        paymentMethodInfo.setNickname(paymentMethod.getNickname());
        paymentMethodInfo.setStatus(paymentMethod.getStatus());
        paymentMethodInfo.setType(paymentMethod.getType());
        paymentMethodInfo.setMetadata(paymentMethod.getMetadata());

        GetTransactionDetailsResponse response = new GetTransactionDetailsResponse();
        response.setTransaction(transaction);
        response.setAccount(accountInfo);
        response.setPaymentMethod(paymentMethodInfo);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Retrieve transaction details successfully!",
            response
        );
    }
}
