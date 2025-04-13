package com.accountservice.repository;

import com.accountservice.model.Balance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepository extends MongoRepository<Balance, String> {
    Optional<Balance> findBalanceByAccountId(String accountId);
    Balance findByAccountId(String accountId);
}
