package com.stocktrading.kafka.model;

import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.model.enums.WithdrawalSagaStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "withdrawal_sagas")
public class WithdrawalSagaState {
    @Id
    private String id;

    @Indexed(unique = true)
    private String sagaId;

    private String userId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
    private String transactionId;
    private String paymentProcessorTransactionId;

    // Saga execution state
    private WithdrawalSagaStep currentStep;
    private SagaStatus status;
    private List<String> completedSteps;
    private List<SagaEvent> sagaEvents;
    private String failureReason;
    private Map<String, Object> stepData;

    // Timing information
    private Instant startTime;
    private Instant endTime;
    private Instant lastUpdatedTime;
    private Instant currentStepStartTime;

    // Retry information
    private int retryCount;
    private int maxRetries;

    public static WithdrawalSagaState initiate(String sagaId, String userId, String accountId,
                                            BigDecimal amount, String currency,
                                            String paymentMethodId, int maxRetries,
                                            String description) {
        HashMap<String, Object> initialPayload = new HashMap<>();

        initialPayload.put("description", description);

        return WithdrawalSagaState.builder()
                .sagaId(sagaId)   // Set the business sagaId field
                .userId(userId)
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .paymentMethodId(paymentMethodId)
                .currentStep(WithdrawalSagaStep.START)
                .status(SagaStatus.STARTED)
                .completedSteps(new ArrayList<>())
                .sagaEvents(new ArrayList<>())
                .stepData(initialPayload)
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(maxRetries)
                .build();
    }
}
