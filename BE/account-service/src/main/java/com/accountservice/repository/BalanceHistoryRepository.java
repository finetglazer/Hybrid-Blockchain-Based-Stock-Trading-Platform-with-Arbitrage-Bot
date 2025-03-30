package com.accountservice.repository;

import com.accountservice.model.BalanceHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BalanceHistoryRepository extends MongoRepository<BalanceHistory, String> {
    BalanceHistory findBalanceHistoryByAccountIdAndDate(String accountId, LocalDate date);
}
