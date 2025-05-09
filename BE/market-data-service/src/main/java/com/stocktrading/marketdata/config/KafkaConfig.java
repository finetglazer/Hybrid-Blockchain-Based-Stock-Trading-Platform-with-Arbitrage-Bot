// src/main/java/com/stocktrading/marketdata/config/KafkaConfig.java
package com.stocktrading.marketdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.marketdata.model.StockUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:market-data-service-group}")
    private String groupId;

    @Value("${kafka.topics.market-price-updates:market.price.updates}")
    private String marketPriceUpdatesTopic;


    @Value("${kafka.topics.market-commands.sell:market.commands.order-sell}")
    private String marketCommandsSellTopic;

    @Value("${kafka.topics.market-events.sell:market.events.order-sell}")
    private String marketEventsSellTopic;


    @Bean
    public ConsumerFactory<String, CommandMessage> commandConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.project.kafkamessagemodels.model.CommandMessage");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CommandMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CommandMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(commandConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Error processing record: {}", exception.getMessage(), exception);
                },
                new FixedBackOff(1000, 3) // retry 3 times with 1s interval
        );

        handler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class,
                java.lang.IllegalStateException.class
        );

        return handler;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic marketPriceUpdatesTopic() {
        return new NewTopic(marketPriceUpdatesTopic, 3, (short) 1);
    }


    @Bean
    public ConsumerFactory<String, EventMessage> eventMessageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.project.kafkamessagemodels.model.EventMessage");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> eventMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventMessageConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }
}