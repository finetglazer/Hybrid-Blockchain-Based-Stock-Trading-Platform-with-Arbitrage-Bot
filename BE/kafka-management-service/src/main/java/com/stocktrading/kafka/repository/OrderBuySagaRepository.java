package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.OrderBuySagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderBuySagaRepository extends MongoRepository<OrderBuySagaState, String> {

    /**
     * Find all sagas with active statuses
     */
    List<OrderBuySagaState> findByStatusIn(List<SagaStatus> statuses);

    /**
     * Find sagas by user ID
     */
    List<OrderBuySagaState> findByUserId(String userId);

    /**
     * Find sagas by account ID
     */
    List<OrderBuySagaState> findByAccountId(String accountId);

    /**
     * Find saga by order ID
     */
    OrderBuySagaState findByOrderId(String orderId);

    /**
     * Find active sagas that haven't been updated recently
     */
    @Query("{ 'status' : { $in : ?0 }, 'lastUpdatedTime' : { $lt : ?1 } }")
    List<OrderBuySagaState> findStaleSagas(List<SagaStatus> statuses, Instant cutoffTime);

    /**
     * Find sagas for timeout detection
     */
    @Query("{ 'status' : { $in : ?0 }, 'currentStepStartTime' : { $lt : ?1 } }")
    List<OrderBuySagaState> findPotentiallyTimedOutSagas(List<SagaStatus> statuses, Instant cutoffTime);
}