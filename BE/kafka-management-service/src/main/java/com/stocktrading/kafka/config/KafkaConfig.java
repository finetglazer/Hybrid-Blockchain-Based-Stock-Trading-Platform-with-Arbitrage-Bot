package com.stocktrading.kafka.config;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
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

    // Default consumer group ID
    @Value("${spring.kafka.consumer.group-id}")
    private String defaultGroupId;

    // Deposit saga topics
    @Value("${kafka.topics.account-commands:account.commands.deposit}")
    private String accountCommandsDepositTopic;

    @Value("${kafka.topics.payment-commands:payment.commands.deposit}")
    private String paymentCommandsDepositTopic;

    @Value("${kafka.topics.user-commands.deposit:user.commands.deposit}")
    private String userCommandsDepositTopic;

    @Value("${kafka.topics.account-events:account.events.deposit}")
    private String accountEventsDepositTopic;

    @Value("${kafka.topics.payment-events:payment.events.deposit}")
    private String paymentEventsDepositTopic;

    @Value("${kafka.topics.user-events.deposit:user.events.deposit}")
    private String userEventsDepositTopic;

    // Order buy saga topics
    @Value("${kafka.topics.user-commands.order-buy:user.commands.order-buy}")
    private String userCommandsOrderBuyTopic;

    @Value("${kafka.topics.user-events.order-buy:user.events.order-buy}")
    private String userEventsOrderBuyTopic;

    @Value("${kafka.topics.account-commands.order-buy:account.commands.order-buy}")
    private String accountCommandsOrderBuyTopic;

    @Value("${kafka.topics.account-events.order-buy:account.events.order-buy}")
    private String accountEventsOrderBuyTopic;

    @Value("${kafka.topics.order-commands:order.commands.order-buy}")
    private String orderCommandsTopic;

    @Value("${kafka.topics.order-events:order.events.order-buy}")
    private String orderEventsTopic;

    @Value("${kafka.topics.market-commands:market.commands.order-buy}")
    private String marketCommandsTopic;

    @Value("${kafka.topics.market-events:market.events.order-buy}")
    private String marketEventsTopic;

    @Value("${kafka.topics.broker-commands:broker.commands.order-buy}")
    private String brokerCommandsTopic;

    @Value("${kafka.topics.broker-events:broker.events.order-buy}")
    private String brokerEventsTopic;

    @Value("${kafka.topics.portfolio-commands.order-buy}")
    private String portfolioCommandsTopic;

    @Value("${kafka.topics.portfolio-events.order-buy}")
    private String portfolioEventsTopic;


    @Value("${kafka.topics.dlq:saga.dlq}")
    private String dlqTopic;

    // Order sell saga topics
    @Value("${kafka.topics.user-commands.order-sell:user.commands.order-sell}")
    private String userCommandsOrderSellTopic;

    @Value("${kafka.topics.user-events.order-sell:user.events.order-sell}")
    private String userEventsOrderSellTopic;

    @Value("${kafka.topics.account-commands.order-sell:account.commands.order-sell}")
    private String accountCommandsOrderSellTopic;

    @Value("${kafka.topics.account-events.order-sell:account.events.order-sell}")
    private String accountEventsOrderSellTopic;

    @Value("${kafka.topics.order-commands.sell:order.commands.order-sell}")
    private String orderCommandsSellTopic;

    @Value("${kafka.topics.order-events.sell:order.events.order-sell}")
    private String orderEventsSellTopic;

    @Value("${kafka.topics.market-commands.sell:market.commands.order-sell}")
    private String marketCommandsSellTopic;

    @Value("${kafka.topics.market-events.sell:market.events.order-sell}")
    private String marketEventsSellTopic;

    @Value("${kafka.topics.broker-commands.sell:broker.commands.order-sell}")
    private String brokerCommandsSellTopic;

    @Value("${kafka.topics.broker-events.sell:broker.events.order-sell}")
    private String brokerEventsSellTopic;

    @Value("${kafka.topics.portfolio-commands.order-sell:portfolio.commands.order-sell}")
    private String portfolioCommandsOrderSellTopic;

    @Value("${kafka.topics.portfolio-events.order-sell:portfolio.events.order-sell}")
    private String portfolioEventsOrderSellTopic;


    // Kafka Admin Configuration
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // Topic Configurations - Deposit Saga
    @Bean
    public NewTopic accountCommandsDepositTopic() {
        return new NewTopic(accountCommandsDepositTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic paymentCommandsDepositTopic() {
        return new NewTopic(paymentCommandsDepositTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic userCommandsDepositTopic() {
        return new NewTopic(userCommandsDepositTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic accountEventsDepositTopic() {
        return new NewTopic(accountEventsDepositTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic paymentEventsDepositTopic() {
        return new NewTopic(paymentEventsDepositTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic userEventsDepositTopic() {
        return new NewTopic(userEventsDepositTopic, 3, (short) 1);
    }

    // Topic Configurations - Order Buy Saga
    @Bean
    public NewTopic userCommandsOrderBuyTopic() {
        return new NewTopic(userCommandsOrderBuyTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic userEventsOrderBuyTopic() {
        return new NewTopic(userEventsOrderBuyTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic accountCommandsOrderBuyTopic() {
        return new NewTopic(accountCommandsOrderBuyTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic accountEventsOrderBuyTopic() {
        return new NewTopic(accountEventsOrderBuyTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderCommandsTopic() {
        return new NewTopic(orderCommandsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return new NewTopic(orderEventsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic marketCommandsTopic() {
        return new NewTopic(marketCommandsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic marketEventsTopic() {
        return new NewTopic(marketEventsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic brokerCommandsTopic() {
        return new NewTopic(brokerCommandsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic brokerEventsTopic() {
        return new NewTopic(brokerEventsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic dlqTopic() {
        return new NewTopic(dlqTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic portfolioCommandsTopic() {
        return new NewTopic(portfolioCommandsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic portfolioEventsTopic() {
        return new NewTopic(portfolioEventsTopic, 3, (short) 1);
    }

    // Topic Configurations - Order Sell Saga
    @Bean
    public NewTopic userCommandsOrderSellTopic() {
        return new NewTopic(userCommandsOrderSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic userEventsOrderSellTopic() {
        return new NewTopic(userEventsOrderSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic accountCommandsOrderSellTopic() {
        return new NewTopic(accountCommandsOrderSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic accountEventsOrderSellTopic() {
        return new NewTopic(accountEventsOrderSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderCommandsSellTopic() {
        return new NewTopic(orderCommandsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderEventsSellTopic() {
        return new NewTopic(orderEventsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic marketCommandsSellTopic() {
        return new NewTopic(marketCommandsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic marketEventsSellTopic() {
        return new NewTopic(marketEventsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic brokerCommandsSellTopic() {
        return new NewTopic(brokerCommandsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic brokerEventsSellTopic() {
        return new NewTopic(brokerEventsSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic portfolioCommandsOrderSellTopic() {
        return new NewTopic(portfolioCommandsOrderSellTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic portfolioEventsOrderSellTopic() {
        return new NewTopic(portfolioEventsOrderSellTopic, 3, (short) 1);
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
        // Add type information to headers
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CommandMessage> commandKafkaTemplate() {
        return new KafkaTemplate<>(commandProducerFactory());
    }

    // Producer for EventMessage
    @Bean
    public ProducerFactory<String, EventMessage> eventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Add type info headers
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, EventMessage> eventKafkaTemplate() {
        return new KafkaTemplate<>(eventProducerFactory());
    }

    // Consumer Configuration for EventMessage - Deposit Saga
    @Bean
    public ConsumerFactory<String, EventMessage> depositEventConsumerFactory() {
        return createEventConsumerFactory(defaultGroupId + "-deposit");
    }

    // Consumer Configuration for EventMessage - Order Buy Saga
    @Bean
    public ConsumerFactory<String, EventMessage> orderBuyEventConsumerFactory() {
        return createEventConsumerFactory(defaultGroupId + "-order-buy");
    }

    // Helper method to create event consumer factory
    private ConsumerFactory<String, EventMessage> createEventConsumerFactory(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        // Set trusted packages
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        // Use correct package for model class
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.project.kafkamessagemodels.model.EventMessage");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Add type info for deserialization
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Main container factory for events - used by default for all event listeners
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> eventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(depositEventConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure error handling with dead letter topic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(eventKafkaTemplate(), (rec, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
                new ExponentialBackOff(1000, 2)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // Order Buy saga specific container factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> orderBuyEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderBuyEventConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure error handling with dead letter topic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(eventKafkaTemplate(), (rec, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
                new ExponentialBackOff(1000, 2)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // Generic error handler bean
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> {
                    System.err.println("Error processing record: " + exception.getMessage());
                    exception.printStackTrace();
                },
                new ExponentialBackOff(1000, 2) // Retry with exponential backoff
        );

        // Add non-retryable exceptions
        handler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );

        return handler;
    }

    // Consumer Configuration for EventMessage - Order Sell Saga
    @Bean
    public ConsumerFactory<String, EventMessage> orderSellEventConsumerFactory() {
        return createEventConsumerFactory(defaultGroupId + "-order-sell");
    }

    // Order Sell saga specific container factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> orderSellEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderSellEventConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure error handling with dead letter topic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(eventKafkaTemplate(), (rec, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
                new ExponentialBackOff(1000, 2)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}