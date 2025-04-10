package com.project.kafkamessagemodels.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommandMessage extends BaseMessage {
    private Map<String, Object> payload = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();
    private Boolean isCompensation = false;
    private String targetService;

    /**
     * Helper method to set payload value
     */
    public void setPayloadValue(String key, Object value) {
        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put(key, value);
    }

    /**
     * Helper method to get payload value
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        if (payload == null) {
            return null;
        }
        return (T) payload.get(key);
    }

    /**
     * Helper method to set metadata value
     */
    public void setMetadataValue(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * Helper method to get metadata value
     */
    public String getMetadataValue(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
}