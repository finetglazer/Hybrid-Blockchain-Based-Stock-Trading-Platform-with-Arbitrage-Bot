package com.project.userservice.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandMessage {
    private String messageId;
    private String sagaId;
    private Integer stepId;
    private String type;
    private Instant timestamp;
    private String sourceService;
    private String targetService;
    private Boolean isCompensation;
    private Map<String, Object> payload = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        if (payload == null) {
            return null;
        }
        return (T) payload.get(key);
    }
}
