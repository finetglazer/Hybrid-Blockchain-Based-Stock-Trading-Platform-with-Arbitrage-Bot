package com.stocktrading.kafka.service;

import com.project.kafkamessagemodels.model.BaseMessage;
import com.stocktrading.kafka.model.ProcessedMessage;
import com.stocktrading.kafka.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

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
        String messageId = message.getMessageId();

        // If messageId is null, check by sagaId and stepId combination
        if (messageId == null) {
            if (message.getSagaId() == null || message.getStepId() == null) {
                return false;
            }
            return processedMessageRepository.findBySagaIdAndStepId(
                    message.getSagaId(), message.getStepId()) != null;
        }

        ProcessedMessage processedMessage = processedMessageRepository.findByMessageId(messageId);
        return processedMessage != null;
    }

    /**
     * Record that a message has been processed
     */
    public void recordProcessing(BaseMessage message, Map<String, Object> result) {
        String messageId = message.getMessageId();
        String sagaId = message.getSagaId();

        // Debug logging to help diagnose the issue
        log.debug("Recording processing for message: messageId={}, sagaId={}, type={}",
                messageId, sagaId, message.getType());

        // Handle null messageId case
        if (messageId == null) {
            // If this is the case in the Kafka UI image, generate a UUID
            // based on sagaId and stepId to ensure uniqueness
            if (sagaId != null && message.getStepId() != null) {
                messageId = UUID.nameUUIDFromBytes(
                        (sagaId + "-" + message.getStepId()).getBytes()
                ).toString();
                log.info("Generated messageId {} for sagaId {} and stepId {}",
                        messageId, sagaId, message.getStepId());
            } else {
                messageId = UUID.randomUUID().toString();
                log.warn("Generated random messageId {} for message with null messageId", messageId);
            }
        }

        ProcessedMessage processedMessage = ProcessedMessage.create(
                messageId,
                sagaId,
                message.getStepId(),
                message.getType(),
                result
        );

        try {
            processedMessageRepository.save(processedMessage);
            log.debug("Successfully recorded message processing: {}", messageId);
        } catch (Exception e) {
            log.error("Failed to record message processing: {}", messageId, e);
        }
    }

    /**
     * Get previously processed result for a message
     */
    public Map<String, Object> getProcessedResult(BaseMessage message) {
        if (message.getMessageId() == null) {
            if (message.getSagaId() != null && message.getStepId() != null) {
                ProcessedMessage processedMessage = processedMessageRepository.findBySagaIdAndStepId(
                        message.getSagaId(), message.getStepId());
                return processedMessage != null ? processedMessage.getResult() : null;
            }
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