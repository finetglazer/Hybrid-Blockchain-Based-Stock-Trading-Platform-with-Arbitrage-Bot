package com.stocktrading.kafka.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API error response
 */
@Data
@Builder
public class ApiError {
    private int status;
    private String message;
    private Map<String, String> errors;

    @Builder.Default
    private Instant timestamp = Instant.now();
}