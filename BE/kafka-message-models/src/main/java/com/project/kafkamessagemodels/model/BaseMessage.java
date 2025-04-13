package com.project.kafkamessagemodels.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMessage {
    private String messageId;
    private String sagaId;
    private Integer stepId;
    private String type;
    private Instant timestamp;
    private String sourceService;
    private Integer version;

    /**
     * Initialize a message with defaults
     */
    public void initialize() {
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (version == null) {
            version = 1;
        }
    }
}