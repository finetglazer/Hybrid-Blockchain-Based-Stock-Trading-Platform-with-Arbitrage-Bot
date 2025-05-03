package com.accountservice.repository;

import com.accountservice.model.TradingAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingAccountRepository extends MongoRepository<TradingAccount, String> {
    Optional<TradingAccount> findTradingAccountById(String id);
    Page<TradingAccount> findTradingAccountsByUserIdAndStatus(String userId, String status, Pageable pageable);
    Page<TradingAccount> findAllByUserId(String userId, Pageable pageable);
    List<TradingAccount> findAllByUserIdAndStatus(String userId, String status);
    List<TradingAccount> findTradingAccountsByUserId(String userId);
}
