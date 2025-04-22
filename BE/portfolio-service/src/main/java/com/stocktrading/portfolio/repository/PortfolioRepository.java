package com.stocktrading.portfolio.repository;

import com.stocktrading.portfolio.model.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

    /**
     * Find portfolios by user ID
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Find portfolios by account ID
     */
    List<Portfolio> findByAccountId(String accountId);

    /**
     * Find portfolio by user ID and account ID
     */
    Optional<Portfolio> findByUserIdAndAccountId(String userId, String accountId);

    /**
     * Check if a portfolio exists for user ID and account ID
     */
    boolean existsByUserIdAndAccountId(String userId, String accountId);
}