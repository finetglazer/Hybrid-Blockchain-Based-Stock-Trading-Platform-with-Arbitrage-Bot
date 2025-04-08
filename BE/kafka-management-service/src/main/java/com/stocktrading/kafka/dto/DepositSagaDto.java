package com.stocktrading.kafka.controller.dto;

import com.stocktrading.kafka.model.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for deposit saga information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositSagaDto {
    private String sagaId;
    private String userId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
    private String transactionId;
    private String currentStep;
    private Integer currentStepNumber;
    private String status;
    private List<String> completedSteps;
    private List<SagaEvent> recentEvents;
    private String failureReason;
    private Instant startTime;
    private Instant endTime;
    private Instant lastUpdatedTime;
    private int retryCount;
    private int maxRetries;
}

---

package com.stocktrading.kafka.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

---

package com.stocktrading.kafka.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

