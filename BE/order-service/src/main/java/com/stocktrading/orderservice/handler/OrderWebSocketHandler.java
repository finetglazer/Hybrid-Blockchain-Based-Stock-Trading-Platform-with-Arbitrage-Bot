package com.stocktrading.orderservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.orderservice.model.Order;
import com.stocktrading.orderservice.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(OrderWebSocketHandler.class);

    // Thread-safe set to keep track of all active sessions
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        logger.info("WebSocket connection established: {}, Total sessions: {}", session.getId(), sessions.size());
        // Optional: Send a welcome message or initial state if needed
        // session.sendMessage(new TextMessage("{\"status\": \"connected\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Overridden to prevent default logging of incoming messages if desired
        // We don't expect messages from the client in this unidirectional setup.
        // You could log it, or send an error back if messages are unexpected.
        logger.warn("Received unexpected message from {}: {}", session.getId(), message.getPayload());
        try {
            session.sendMessage(new TextMessage("{\"warning\": \"Messages from client are not processed.\"}"));
        } catch (IOException e) {
            logger.error("Failed to send warning message to session {}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session); // Ensure removal on error
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} with status {}, Total sessions: {}", session.getId(), status, sessions.size());
    }

    @KafkaListener(
        topics = "${kafka.topics.order-events}", // Topic for general order updates
        containerFactory = "kafkaListenerContainerFactory"
    )
    // Renamed method to accurately reflect its purpose
    public void broadcastOrderStatusUpdate(EventMessage event) {
        // ... inside broadcastOrderStatusUpdate method ...

        Object rawOrder = event.getPayloadValue("order");
        if (rawOrder == null) {
            logger.warn("Received event with null 'order' payload. Skipping.");
            return;
        }

// --- DIAGNOSTIC LOGGING ---
        if (rawOrder instanceof java.util.Map) {
            @SuppressWarnings("unchecked") // Be careful with raw types if possible
            java.util.Map<String, Object> orderMap = (java.util.Map<String, Object>) rawOrder;
            Object createdAtValue = orderMap.get("createdAt");
            if (createdAtValue != null) {
                logger.info("Diagnosing 'createdAt': Value = [{}], Type = [{}]",
                        createdAtValue, createdAtValue.getClass().getName());
            } else {
                logger.warn("Diagnosing 'createdAt': Field is null in rawOrder map.");
            }
        } else {
            logger.warn("Diagnosing 'createdAt': rawOrder is not a Map. Type = [{}]", rawOrder.getClass().getName());
            // Log rawOrder itself if it's small enough and not sensitive
            // logger.info("Raw order object: {}", rawOrder);
        }
// --- END DIAGNOSTIC LOGGING ---


        ObjectMapper localMapper = new ObjectMapper(); // Use a local mapper for this specific task
        localMapper.registerModule(new JavaTimeModule());
// Optional: If you expect ISO strings and NOT numeric timestamps, add this:
// localMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

        OrderStatus orderStatus;
        try {
            // This is the line causing the error (around line 80)
            orderStatus = localMapper.convertValue(rawOrder, OrderStatus.class);
        } catch (IllegalArgumentException e) {
            // Log the specific error during conversion
            logger.error("Failed to convert raw payload to OrderStatus. Error: {}", e.getMessage(), e);
            // Log rawOrder again here for context if debugging
            // logger.error("Problematic rawOrder payload: {}", rawOrder);
            return; // Stop processing if conversion fails
        } catch (Exception e) { // Catch broader exceptions during conversion just in case
            logger.error("Unexpected error during OrderStatus conversion. Error: {}", e.getMessage(), e);
            return;
        }

// --- The rest of your code (serialization and broadcasting) ---
// You should still use a properly configured ObjectMapper for serialization too.
// Either use the 'localMapper' created above, or ensure your shared 'objectMapper'
// (if used later) is also configured with JavaTimeModule.

        String orderId = orderStatus.getId();
        TextMessage message;

        try {
            // Use the SAME localMapper OR a correctly configured shared objectMapper
            message = new TextMessage(localMapper.writeValueAsString(orderStatus));
            // If using a shared/injected objectMapper:
            // message = new TextMessage(objectMapper.writeValueAsString(orderStatus));
            // Make SURE that shared objectMapper also has JavaTimeModule registered globally!
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize OrderStatus for orderId {}: {}", orderId, e.getMessage(), e);
            return;
        }

        // --- WebSocket Broadcasting Logic (Same as before) ---
        int sentCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                    sentCount++;
                } catch (IOException e) {
                    logger.error("Failed to send OrderStatus update for orderId {} to session {}. Error: {}",
                            orderId, session.getId(), e.getMessage());
                } catch (IllegalStateException e) {
                    logger.warn("Session {} closed unexpectedly before message could be sent for orderId {}. Error: {}",
                            session.getId(), orderId, e.getMessage());
                    sessions.remove(session);
                }
            } else {
                logger.debug("Removing closed session found during broadcast: {}", session.getId());
                sessions.remove(session);
            }
        }

        if (sentCount > 0) {
            logger.trace("Broadcasted OrderStatus update for orderId {} to {} sessions", orderId, sentCount);
        } else {
            logger.trace("No active sessions found to broadcast OrderStatus update for orderId {} to.", orderId);
        }
    }
}
