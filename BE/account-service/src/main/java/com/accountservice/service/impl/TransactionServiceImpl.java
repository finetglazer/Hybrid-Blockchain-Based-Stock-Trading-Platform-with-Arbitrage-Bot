package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.common.Const;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.repository.TransactionRepository;
import com.accountservice.service.TransactionService;
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
import java.util.List;


@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;

    private final MongoTemplate mongoTemplate;

    @Override
    public BaseResponse<List<Transaction>> getTransactions(GetTransactionsRequest getTransactionsRequest) {
        String accountId = getTransactionsRequest.getAccountId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(getTransactionsRequest.getStartDate(), formatter);
        LocalDate endDate = LocalDate.parse(getTransactionsRequest.getEndDate(), formatter);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(23, 59, 59);
        List<String> statuses = getTransactionsRequest.getStatuses();
        List<String> types = getTransactionsRequest.getTypes();
        Integer page = getTransactionsRequest.getPage();
        Integer size = getTransactionsRequest.getSize();

        Criteria accountIdCriteria = Criteria.where("accountId").is(accountId);
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
        Criteria timeCriteria = Criteria.where("startTime").gte(startTime)
                                .and("endTime").lte(endTime);

        Query query = new Query(
                accountIdCriteria
                .andOperator(timeCriteria)
        );
        if (!statuses.isEmpty()) {
            query.addCriteria(statusCriteria);
        }
        if (!types.isEmpty()) {
            query.addCriteria(typeCriteria);
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
