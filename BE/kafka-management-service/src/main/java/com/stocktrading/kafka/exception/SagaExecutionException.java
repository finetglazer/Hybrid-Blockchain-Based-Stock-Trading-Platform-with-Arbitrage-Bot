package com.stocktrading.kafka.exception;

/**
 * Exception thrown when there's an error executing a saga
 */
public class SagaExecutionException extends RuntimeException {

    public SagaExecutionException(String message) {
        super(message);
    }

    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}