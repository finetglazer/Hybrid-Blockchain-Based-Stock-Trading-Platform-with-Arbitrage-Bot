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
public class EventMessage {
    private String messageId;
    private String sagaId;
    private Integer stepId;
    private String type;
    private Instant timestamp;
    private String sourceService;
    private Boolean success = true;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> payload = new HashMap<>();

    public void setPayloadValue(String key, Object value) {
        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put(key, value);
    }
}