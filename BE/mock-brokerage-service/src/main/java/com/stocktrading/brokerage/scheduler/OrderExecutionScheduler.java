//package com.stocktrading.brokerage.scheduler;
//
//import com.project.kafkamessagemodels.model.EventMessage;
//import com.stocktrading.brokerage.model.MockOrderBook;
//import com.stocktrading.brokerage.model.PendingOrder;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Scheduler for updating prices and checking for executable orders
// */
//@Slf4j
//@Component
//@EnableScheduling
//@RequiredArgsConstructor
//public class OrderExecutionScheduler {
//
//    private final MockOrderBook mockOrderBook;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Value("${kafka.topics.broker-events}")
//    private String brokerEventsTopic;
//
//    /**
//     * Run price updates and order checks every 5 seconds
//     */
//    @Scheduled(fixedRate = 5000)
//    public void updatePricesAndCheckOrders() {
//        log.debug("Running scheduled price update and order check");
//
//        try {
//            // Update all stock prices to simulate market movement
//            mockOrderBook.updatePrices();
//
//            // Check for orders that can be executed at new prices
//            processExecutableOrders();
//
//            // Check for expired orders
//            processExpiredOrders();
//
//        } catch (Exception e) {
//            log.error("Error in price update scheduler", e);
//        }
//    }
//
//    /**
//     * Process orders that can be executed at current prices
//     */
//    private void processExecutableOrders() {
//        List<PendingOrder> executableOrders = mockOrderBook.findExecutableOrders();
//
//        for (PendingOrder order : executableOrders) {
//            try {
//                // Calculate execution price (different for buy/sell)
//                BigDecimal executionPrice = mockOrderBook.getExecutionPrice(
//                        order.getStockSymbol(), order.getSide());
//
//                // Remove order from the book
//                mockOrderBook.removePendingOrder(order.getOrderId());
//
//                // Create executed event
//                EventMessage event = EventMessage.builder()
//                        .messageId(UUID.randomUUID().toString())
//                        .sagaId(order.getSagaId())
//                        .type("ORDER_EXECUTED_BY_BROKER")
//                        .sourceService("MOCK_BROKERAGE_SERVICE")
//                        .timestamp(Instant.now())
//                        .success(true)
//                        .build();
//
//                // Set payload
//                event.setPayloadValue("orderId", order.getOrderId());
//                event.setPayloadValue("brokerOrderId", "MBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
//                event.setPayloadValue("stockSymbol", order.getStockSymbol());
//                event.setPayloadValue("executionPrice", executionPrice);
//                event.setPayloadValue("executedQuantity", order.getQuantity());
//                event.setPayloadValue("executedAt", Instant.now().toString());
//                event.setPayloadValue("status", "FILLED");
//
//                // Send the event
//                kafkaTemplate.send(brokerEventsTopic, order.getSagaId(), event);
//
//                log.info("Order executed and event sent: {}", order.getOrderId());
//
//            } catch (Exception e) {
//                log.error("Error executing order: {}", order.getOrderId(), e);
//            }
//        }
//    }
//
//    /**
//     * Process orders that have expired
//     */
//    private void processExpiredOrders() {
//        List<PendingOrder> expiredOrders = mockOrderBook.findExpiredOrders();
//
//        for (PendingOrder order : expiredOrders) {
//            try {
//                // Remove order from the book
//                mockOrderBook.removePendingOrder(order.getOrderId());
//
//                // Create expired event
//                EventMessage event = EventMessage.builder()
//                        .messageId(UUID.randomUUID().toString())
//                        .sagaId(order.getSagaId())
//                        .type("ORDER_EXPIRED")
//                        .sourceService("MOCK_BROKERAGE_SERVICE")
//                        .timestamp(Instant.now())
//                        .success(true)
//                        .build();
//
//                // Set payload
//                event.setPayloadValue("orderId", order.getOrderId());
//                event.setPayloadValue("stockSymbol", order.getStockSymbol());
//                event.setPayloadValue("expiredAt", Instant.now().toString());
//                event.setPayloadValue("status", "EXPIRED");
//
//                // Send the event
//                kafkaTemplate.send(brokerEventsTopic, order.getSagaId(), event);
//
//                log.info("Order expired and event sent: {}", order.getOrderId());
//
//            } catch (Exception e) {
//                log.error("Error processing expired order: {}", order.getOrderId(), e);
//            }
//        }
//    }
//}
