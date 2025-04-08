package com.stocktrading.kafka.exception;

/**
 * Exception thrown when a saga is not found
 */
public class SagaNotFoundException extends RuntimeException {

    public SagaNotFoundException(String sagaId) {
        super("Saga not found with ID: " + sagaId);
    }
}