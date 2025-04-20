package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.WithdrawalSagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalSagaRepository extends MongoRepository<WithdrawalSagaState, String> {
    Optional<WithdrawalSagaState> getWithdrawalSagaStateBySagaId(String sagaId);

    /**
     * Find sagas for timeout detection
     */
    @Query("{ 'status' : { $in : ?0 }, 'currentStepStartTime' : { $lt : ?1 } }")
    List<WithdrawalSagaState> findPotentiallyTimedOutSagas(List<SagaStatus> statuses, Instant cutoffTime);
}
