package com.stocktrading.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_messages")
public class ProcessedMessage {
    @Id
    private String id; // This will be stored as MongoDB's _id field

    @Indexed
    private String messageId; // Separate field for messageId

    @Indexed
    private String sagaId;

    private Integer stepId;
    private String messageType;
    private Instant processedAt;
    private Map<String, Object> result;

    public static ProcessedMessage create(String messageId, String sagaId, Integer stepId,
                                          String messageType, Map<String, Object> result) {
        return ProcessedMessage.builder()
                .id(messageId) // Use messageId as the MongoDB _id
                .messageId(messageId) // Also set a separate messageId field
                .sagaId(sagaId)
                .stepId(stepId)
                .messageType(messageType)
                .processedAt(Instant.now())
                .result(result)
                .build();
    }
}