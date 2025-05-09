package com.stocktrading.kafka.dto;

import com.stocktrading.kafka.model.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for order sell saga information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSellSagaDto {
    private String sagaId;
    private String userId;
    private String accountId;
    private String stockSymbol;
    private String orderType;
    private Integer quantity;
    private BigDecimal limitPrice;
    private String timeInForce;
    private String orderId;
    private BigDecimal executionPrice;
    private Integer executedQuantity;
    private Integer reservedQuantity;
    private String reservationId;
    private BigDecimal settlementAmount;
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