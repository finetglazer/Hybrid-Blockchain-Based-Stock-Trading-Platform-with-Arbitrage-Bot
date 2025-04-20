package com.stocktrading.kafka.listener;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.kafka.service.DepositSagaService;
import com.stocktrading.kafka.service.IdempotencyService;
import com.stocktrading.kafka.service.OrderBuySagaService;
import com.stocktrading.kafka.service.WithdrawalSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for processing event messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final DepositSagaService depositSagaService;
    private final WithdrawalSagaService withdrawalSagaService;
    private final OrderBuySagaService orderBuySagaService;
    private final IdempotencyService idempotencyService;

    // ====== DEPOSIT SAGA EVENT LISTENERS ======
    @KafkaListener(
            topics = "${kafka.topics.account-events.common}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-account-common"
    )
    public void consumeAccountCommonEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account common event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            withdrawalSagaService.handleEventMessage(event);
            // Acknowledge the message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing account common event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.user-events.common}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-user-common"
    )
    public void consumeUserCommonEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received user common event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            withdrawalSagaService.handleEventMessage(event);
            // Acknowledge the message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user common event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.account-events.deposit}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-deposit-account"
    )
    public void consumeAccountDepositEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account deposit event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            // Acknowledge the message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing account deposit event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-events.deposit}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-deposit-payment"
    )
    public void consumePaymentDepositEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received payment deposit event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment deposit event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    // ====== WITHDRAWAL SAGA EVENT LISTENERS ======
    @KafkaListener(
            topics = "${kafka.topics.account-events.withdrawal}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-withdrawal-account"
    )
    public void consumeAccountWithdrawalEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account withdrawal event: {}", event.getType());
            withdrawalSagaService.handleEventMessage(event);
            // Acknowledge the message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing account withdrawal event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-events.withdrawal}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-withdrawal-payment"
    )
    public void consumePaymentWithdrawalEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received payment withdrawal event: {}", event.getType());
            withdrawalSagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment withdrawal event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    // ====== ORDER BUY SAGA EVENT LISTENERS ======

    @KafkaListener(
            topics = "${kafka.topics.user-events.order-buy}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-user"
    )
    public void consumeUserOrderBuyEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received user order-buy event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
            log.debug("Successfully processed and acknowledged user order-buy event: {}", event.getType());
        } catch (Exception e) {
            log.error("Error processing user order-buy event: {}", event.getType(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.account-events.order-buy}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-account"
    )
    public void consumeAccountOrderBuyEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account order-buy event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing account order-buy event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-order"
    )
    public void consumeOrderEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received order event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.market-events}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-market"
    )
    public void consumeMarketEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received market event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing market event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.broker-events}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-broker"
    )
    public void consumeBrokerEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received broker event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing broker event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.portfolio-events.order-buy}",
            containerFactory = "orderBuyEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-buy-portfolio"
    )
    public void consumePortfolioEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received portfolio event: {}", event.getType());
            orderBuySagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing portfolio event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }


    // ====== GENERAL DLQ LISTENER ======

    @KafkaListener(
            topics = "${kafka.topics.dlq}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeDlqMessages(@Payload Object messagePayload, Acknowledgment ack) {
        try {
            log.warn("Received message in DLQ: {}", messagePayload);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop in DLQ processing
            ack.acknowledge();
        }
    }
}
