package com.stocktrading.marketdata.repository;

import com.stocktrading.marketdata.model.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {
    Optional<Portfolio> findPortfolioByAccountIdAndUserId(String accountId, String userId);

    List<Portfolio> getPortfoliosByUserId(String userId);
}
