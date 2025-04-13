package com.stocktrading.kafka.service;


import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Service for publishing messages to Kafka topics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessagePublisher {
    
    private final KafkaTemplate<String, CommandMessage> commandKafkaTemplate;
    private final KafkaTemplate<String, EventMessage> eventKafkaTemplate;
    
    /**
     * Publish a command message to a topic
     */
    public void publishCommand(CommandMessage command, String topic) {
        if (command.getMessageId() == null) {
            command.initialize();
        }
        
        // Use the sagaId as the message key to ensure all messages for the same saga
        // are routed to the same partition
        String key = command.getSagaId();
        
        log.debug("Publishing command [{}] to topic: {}, key: {}", command.getType(), topic, key);
        
        ListenableFuture<SendResult<String, CommandMessage>> future = 
            commandKafkaTemplate.send(topic, key, command);
            
        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<String, CommandMessage> result) {
                log.debug("Sent command [{}] for saga [{}] to topic: {} with offset: {}", 
                    command.getType(), command.getSagaId(), topic, 
                    result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Unable to send command [{}] for saga [{}] to topic: {}", 
                    command.getType(), command.getSagaId(), topic, ex);
            }
        });
    }
    
    /**
     * Publish an event message to a topic
     */
    public void publishEvent(EventMessage event, String topic) {
        if (event.getMessageId() == null) {
            event.initialize();
        }
        
        // Use the sagaId as the message key
        String key = event.getSagaId();
        
        log.debug("Publishing event [{}] to topic: {}, key: {}", event.getType(), topic, key);
        
        ListenableFuture<SendResult<String, EventMessage>> future = 
            eventKafkaTemplate.send(topic, key, event);
            
        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<String, EventMessage> result) {
                log.debug("Sent event [{}] for saga [{}] to topic: {} with offset: {}", 
                    event.getType(), event.getSagaId(), topic, 
                    result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Unable to send event [{}] for saga [{}] to topic: {}", 
                    event.getType(), event.getSagaId(), topic, ex);
            }
        });
    }
}
