package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.OrderSellSagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderSellSagaRepository extends MongoRepository<OrderSellSagaState, String> {

    /**
     * Find all sagas with specific statuses
     */
    List<OrderSellSagaState> findByStatusIn(List<SagaStatus> statuses);

    /**
     * Find sagas by user ID
     */
    List<OrderSellSagaState> findByUserId(String userId);

    /**
     * Find sagas by account ID
     */
    List<OrderSellSagaState> findByAccountId(String accountId);

    /**
     * Find saga by order ID
     */
    OrderSellSagaState findByOrderId(String orderId);

    /**
     * Find active sagas that haven't been updated recently
     */
    @Query("{ 'status' : { $in : ?0 }, 'lastUpdatedTime' : { $lt : ?1 } }")
    List<OrderSellSagaState> findStaleSagas(List<SagaStatus> statuses, Instant cutoffTime);

    /**
     * Find sagas for timeout detection
     */
    @Query("{ 'status' : { $in : ?0 }, 'currentStepStartTime' : { $lt : ?1 } }")
    List<OrderSellSagaState> findPotentiallyTimedOutSagas(List<SagaStatus> statuses, Instant cutoffTime);
}