package com.stocktrading.kafka.config;

import com.stocktrading.kafka.model.CommandMessage;
import com.stocktrading.kafka.model.EventMessage;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${kafka.topics.account-commands}")
    private String accountCommandsTopic;
    
    @Value("${kafka.topics.payment-commands}")
    private String paymentCommandsTopic;
    
    @Value("${kafka.topics.user-commands}")
    private String userCommandsTopic;
    
    @Value("${kafka.topics.account-events}")
    private String accountEventsTopic;
    
    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;
    
    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;
    
    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    // Kafka Admin Configuration
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    // Topic Configuration
    @Bean
    public NewTopic accountCommandsTopic() {
        return new NewTopic(accountCommandsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic paymentCommandsTopic() {
        return new NewTopic(paymentCommandsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic userCommandsTopic() {
        return new NewTopic(userCommandsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic accountEventsTopic() {
        return new NewTopic(accountEventsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic paymentEventsTopic() {
        return new NewTopic(paymentEventsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic userEventsTopic() {
        return new NewTopic(userEventsTopic, 3, (short) 1);
    }
    
    @Bean
    public NewTopic dlqTopic() {
        return new NewTopic(dlqTopic, 1, (short) 1);
    }

    // Producer Configuration for CommandMessage
    @Bean
    public ProducerFactory<String, CommandMessage> commandProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CommandMessage> commandKafkaTemplate() {
        return new KafkaTemplate<>(commandProducerFactory());
    }

    // Consumer Configuration for EventMessage
    @Bean
    public ConsumerFactory<String, EventMessage> eventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.stocktrading.kafka.model");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> eventKafkaListenerContainerFactory(
            KafkaTemplate<String, EventMessage> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Configure error handling with dead letter topic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate, (rec, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
                new ExponentialBackOff(1000, 2)
        );
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }
    
    // Producer for EventMessage (for DLQ)
    @Bean
    public ProducerFactory<String, EventMessage> eventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, EventMessage> eventKafkaTemplate() {
        return new KafkaTemplate<>(eventProducerFactory());
    }
}
