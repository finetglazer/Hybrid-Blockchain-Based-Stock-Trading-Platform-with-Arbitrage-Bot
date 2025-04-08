package com.stocktrading.kafka.service;

import com.stocktrading.kafka.model.BaseMessage;
import com.stocktrading.kafka.model.ProcessedMessage;
import com.stocktrading.kafka.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service for ensuring idempotent message processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final ProcessedMessageRepository processedMessageRepository;
    
    /**
     * Check if a message has been processed before
     */
    public boolean isProcessed(BaseMessage message) {
        if (message.getMessageId() == null) {
            return false;
        }
        
        ProcessedMessage processedMessage = processedMessageRepository.findByMessageId(message.getMessageId());
        return processedMessage != null;
    }
    
    /**
     * Record that a message has been processed
     */
    public void recordProcessing(BaseMessage message, Map<String, Object> result) {
        if (message.getMessageId() == null) {
            log.warn("Cannot record processing for message with null messageId");
            return;
        }
        
        ProcessedMessage processedMessage = ProcessedMessage.create(
                message.getMessageId(),
                message.getSagaId(),
                message.getStepId(),
                message.getType(),
                result
        );
        
        try {
            processedMessageRepository.save(processedMessage);
        } catch (Exception e) {
            log.error("Failed to record message processing: {}", message.getMessageId(), e);
        }
    }
    
    /**
     * Get previously processed result for a message
     */
    public Map<String, Object> getProcessedResult(BaseMessage message) {
        if (message.getMessageId() == null) {
            return null;
        }
        
        ProcessedMessage processedMessage = processedMessageRepository.findByMessageId(message.getMessageId());
        if (processedMessage == null) {
            return null;
        }
        
        return processedMessage.getResult();
    }
    
    /**
     * Get previously processed result for a saga step
     */
    public Map<String, Object> getProcessedResultForStep(String sagaId, Integer stepId) {
        ProcessedMessage processedMessage = processedMessageRepository.findBySagaIdAndStepId(sagaId, stepId);
        if (processedMessage == null) {
            return null;
        }
        
        return processedMessage.getResult();
    }
    
    /**
     * Scheduled task to clean up old processed messages
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupOldMessages() {
        log.info("Starting cleanup of old processed messages");
        
        // Keep messages for 14 days
        Instant cutoffTime = Instant.now().minus(14, ChronoUnit.DAYS);
        
        try {
            processedMessageRepository.deleteByProcessedAtBefore(cutoffTime);
            log.info("Completed cleanup of old processed messages");
        } catch (Exception e) {
            log.error("Failed to cleanup old processed messages", e);
        }
    }
}
