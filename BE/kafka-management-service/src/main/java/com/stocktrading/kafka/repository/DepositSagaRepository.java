package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.DepositSagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepositSagaRepository extends MongoRepository<DepositSagaState, String> {
    
    /**
     * Find all sagas with active statuses
     */
    List<DepositSagaState> findByStatusIn(List<SagaStatus> statuses);
    
    /**
     * Find sagas by user ID
     */
    List<DepositSagaState> findByUserId(String userId);
    
    /**
     * Find sagas by account ID
     */
    List<DepositSagaState> findByAccountId(String accountId);
    
    /**
     * Find sagas by transaction ID
     */
    DepositSagaState findByTransactionId(String transactionId);
    
    /**
     * Find active sagas that haven't been updated recently
     */
    @Query("{ 'status' : { $in : ?0 }, 'lastUpdatedTime' : { $lt : ?1 } }")
    List<DepositSagaState> findStaleSagas(List<SagaStatus> statuses, Instant cutoffTime);
    
    /**
     * Find sagas for timeout detection
     */
    @Query("{ 'status' : { $in : ?0 }, 'currentStepStartTime' : { $lt : ?1 } }")
    List<DepositSagaState> findPotentiallyTimedOutSagas(List<SagaStatus> statuses, Instant cutoffTime);

    Optional<DepositSagaState> getDepositSagaStateBySagaId(String sagaId);
}
