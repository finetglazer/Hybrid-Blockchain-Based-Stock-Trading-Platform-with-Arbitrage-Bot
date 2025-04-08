package com.stocktrading.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an event that occurred during saga execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {
    private String type;
    private String description;
    private Instant timestamp;
    
    public static SagaEvent of(String type, String description) {
        return SagaEvent.builder()
            .type(type)
            .description(description)
            .timestamp(Instant.now())
            .build();
    }
}
