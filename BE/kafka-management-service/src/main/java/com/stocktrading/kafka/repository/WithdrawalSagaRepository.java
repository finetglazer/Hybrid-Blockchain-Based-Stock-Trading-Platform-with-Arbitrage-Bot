package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.WithdrawalSagaState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalSagaRepository extends MongoRepository<WithdrawalSagaState, String> {
}
