package com.stocktrading.kafka.model.enums;

/**
 * Enum defining all possible saga status values
 */
public enum SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATION_COMPLETED
}