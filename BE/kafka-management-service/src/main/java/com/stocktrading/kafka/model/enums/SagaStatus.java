package com.stocktrading.kafka.model.enums;

/**
 * Enum defining possible saga statuses
 */
public enum SagaStatus {
    STARTED,            // Saga has been initialized
    IN_PROGRESS,        // Saga is being processed
    COMPLETED,          // Saga completed successfully
    FAILED,             // Saga failed due to an error
    COMPENSATING,       // Saga is executing compensation transactions
    COMPENSATION_COMPLETED, // Saga compensation completed
    LIMIT_ORDER_PENDING,    // Saga is waiting for limit order price conditions
    CANCELLED_BY_USER       // Saga was cancelled by user request
}