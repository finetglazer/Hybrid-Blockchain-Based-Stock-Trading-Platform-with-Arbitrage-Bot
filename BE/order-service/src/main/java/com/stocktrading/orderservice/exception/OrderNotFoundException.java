package com.stocktrading.orderservice.exception;

/**
 * Exception for order not found
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
}

/**
 * Exception for invalid order operations
 */
class InvalidOrderOperationException extends RuntimeException {
    public InvalidOrderOperationException(String message) {
        super(message);
    }
}

/**
 * Exception for order validation errors
 */
class OrderValidationException extends RuntimeException {
    public OrderValidationException(String message) {
        super(message);
    }
}

/**
 * Exception for order execution errors
 */
class OrderExecutionException extends RuntimeException {
    public OrderExecutionException(String message) {
        super(message);
    }
}