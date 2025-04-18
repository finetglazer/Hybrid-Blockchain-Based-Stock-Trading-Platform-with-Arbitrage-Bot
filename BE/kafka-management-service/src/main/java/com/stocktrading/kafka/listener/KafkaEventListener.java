package com.stocktrading.kafka.listener;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.kafka.service.DepositSagaService;
import com.stocktrading.kafka.service.IdempotencyService;
import com.stocktrading.kafka.service.OrderBuySagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final DepositSagaService depositSagaService;
    private final OrderBuySagaService orderBuySagaService;
    private final IdempotencyService idempotencyService;

    // ====== DEPOSIT SAGA EVENT LISTENERS ======

    @KafkaListener(
            topics = "${kafka.topics.account-events}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-deposit-account"
    )
    public void consumeAccountDepositEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account deposit event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing account deposit event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
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

    @KafkaListener(
            topics = "${kafka.topics.user-events.deposit}",
            containerFactory = "eventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-deposit-user"
    )
    public void consumeUserDepositEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received user deposit event: {}", event.getType());
            depositSagaService.handleEventMessage(event);
            ack.acknowledge();
            log.debug("Successfully processed and acknowledged user deposit event: {}", event.getType());
        } catch (Exception e) {
            log.error("Error processing user deposit event: {}", event.getType(), e);
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