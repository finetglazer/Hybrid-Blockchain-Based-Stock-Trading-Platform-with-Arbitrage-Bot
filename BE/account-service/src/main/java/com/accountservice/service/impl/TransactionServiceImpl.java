package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.common.Const;
import com.accountservice.model.TradingAccount;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.service.TransactionService;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final MongoTemplate mongoTemplate;

    @Override
    public BaseResponse<List<Transaction>> getTransactions(GetTransactionsRequest getTransactionsRequest) {
        String userId = getTransactionsRequest.getUserId();
        List<String> accountIds = getTransactionsRequest.getAccountIds() == null ? List.of() : getTransactionsRequest.getAccountIds();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(getTransactionsRequest.getStartDate() == null ? "1970-01-01" : getTransactionsRequest.getStartDate(), formatter);
        LocalDate endDate = LocalDate.parse(getTransactionsRequest.getEndDate() == null ? "9999-12-31" : getTransactionsRequest.getEndDate(), formatter);
        LocalDateTime startLocalDateTime = startDate.atStartOfDay();
        LocalDateTime endLocalDateTime = endDate.atTime(23, 59, 59);
        Date startTime = DateUtils.convertLocalDateTimeToDate(startLocalDateTime);
        Date endTime = DateUtils.convertLocalDateTimeToDate(endLocalDateTime);
        List<String> statuses = getTransactionsRequest.getStatuses() == null ? List.of() : getTransactionsRequest.getStatuses();
        List<String> types = getTransactionsRequest.getTypes() == null ? List.of() : getTransactionsRequest.getTypes();
        List<String> paymentMethodIds = getTransactionsRequest.getPaymentMethodIds();
        int page = getTransactionsRequest.getPage() == null ? 0 : getTransactionsRequest.getPage();
        int size = getTransactionsRequest.getSize() == null ? 10000 : getTransactionsRequest.getSize();

        Criteria statusCriteria = Criteria.where("status").in(
                Arrays.stream(Transaction.TransactionStatus.values())
                        .toList().stream().map(Transaction.TransactionStatus::name).toList()
        );
        Criteria typeCriteria = Criteria.where("type").in(
                Arrays.stream(Transaction.TransactionType.values())
                        .toList().stream().map(Transaction.TransactionType::name).toList()
        );
        for (String status : statuses) {
            Criteria subStatusCriteria = Criteria.where("status").is(status);
            statusCriteria.orOperator(subStatusCriteria);
        }
        for (String type : types) {
            Criteria subTypeCriteria = Criteria.where("type").is(type);
            typeCriteria.orOperator(subTypeCriteria);
        }
        Criteria timeCriteria = Criteria.where("createdAt").gte(startTime).lte(endTime);

        Query query = new Query(timeCriteria);
        if (!accountIds.isEmpty()) {
            query.addCriteria(Criteria.where("accountId").in(accountIds));
        }
        else {
            List<TradingAccount> tradingAccounts = mongoTemplate.find(new Query(Criteria.where("userId").is(userId)), TradingAccount.class);
            query.addCriteria(Criteria.where("accountId").in(tradingAccounts.stream().map(TradingAccount::getId).toList()));
        }
        if (!statuses.isEmpty()) {
            query.addCriteria(statusCriteria);
        }
        if (!types.isEmpty()) {
            query.addCriteria(typeCriteria);
        }
        if (!paymentMethodIds.isEmpty()) {
            query.addCriteria(Criteria.where("paymentMethodId").in(paymentMethodIds));
        }

        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);

        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Transactions retrieved successfully",
            transactions
        );
    }

    public Transaction getLastTransaction(String accountId) {
        Query query = new Query(Criteria.where("accountId").is(accountId));
        query.with(Sort.by(Sort.Order.desc("createdAt")));
        query.limit(1);

        return mongoTemplate.findOne(query, Transaction.class);
    }
}
