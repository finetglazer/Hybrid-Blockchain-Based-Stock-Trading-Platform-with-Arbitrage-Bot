package com.stocktrading.kafka.repository;

import com.stocktrading.kafka.model.ProcessedMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProcessedMessageRepository extends MongoRepository<ProcessedMessage, String> {
    
    /**
     * Find processed message by message ID
     */
    ProcessedMessage findByMessageId(String messageId);
    
    /**
     * Find processed messages by saga ID
     */
    List<ProcessedMessage> findBySagaId(String sagaId);
    
    /**
     * Find processed messages by saga ID and step ID
     */
    ProcessedMessage findBySagaIdAndStepId(String sagaId, Integer stepId);
    
    /**
     * Delete messages older than a certain time
     */
    void deleteByProcessedAtBefore(Instant cutoffTime);
}
