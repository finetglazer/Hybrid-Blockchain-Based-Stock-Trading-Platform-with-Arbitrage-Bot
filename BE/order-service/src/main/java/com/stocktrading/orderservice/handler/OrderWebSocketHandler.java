package com.stocktrading.orderservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.orderservice.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Objects;
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
    public void broadcastOrderUpdate(EventMessage event) {
        if (event.getPayloadValue("order") == null) {
            logger.error("11111111111");
            return;
        }
        logger.error("Order: {}", Optional.ofNullable(event.getPayloadValue("order")));

        ObjectMapper mapper = new ObjectMapper();

        Object rawOrder = event.getPayloadValue("order");

        Order order = mapper.convertValue(rawOrder, Order.class);

        String orderId = order.getId();
        TextMessage message;

        try {
            // Convert the Order object to a JSON string
            message = new TextMessage(objectMapper.writeValueAsString(order));
        } catch (JsonProcessingException e) {
            // Updated log message
            logger.error("Failed to serialize Order for orderId {}: {}", orderId, e.getMessage(), e);
            return;
        }

        // Iterate over a snapshot of the sessions using a thread-safe iterator
        int sentCount = 0;
        // Use local copy to avoid potential issues if sessions set is modified elsewhere during broadcast
        // CopyOnWriteArraySet's iterator is already safe against concurrent modification during iteration itself.

        for (WebSocketSession session : sessions) {
            // Double-check if session is still open before sending
            if (session.isOpen()) {
                try {
                    // Send the JSON message to the client
                    session.sendMessage(message);
                    sentCount++;
                } catch (IOException e) {
                    logger.error("Failed to send OrderStatus update for orderId {} to session {}. Error: {}",
                            orderId, session.getId(), e.getMessage());
                    // Consider removing the session only if errors persist,
                    // relying primarily on afterConnectionClosed callback.
                    // Removing here might be premature if it's a temporary network issue.
                    // If you *do* remove here, ensure 'sessions' is thread-safe.
                    // sessions.remove(session); // Use with caution, CopyOnWriteArraySet remove is safe but potentially slow if frequent.
                } catch (IllegalStateException e) {
                    // Handle cases where session might close between isOpen() check and sendMessage()
                    logger.warn("Session {} closed before message could be sent for orderId {}. Error: {}",
                            session.getId(), orderId, e.getMessage());
                    // Clean up session if needed (though afterConnectionClosed should handle it)
                    sessions.remove(session); // Safe with CopyOnWriteArraySet
                }
            } else {
                // Optional: Proactively remove sessions found closed during broadcast
                logger.debug("Removing closed session found during broadcast: {}", session.getId());
                sessions.remove(session); // Safe with CopyOnWriteArraySet
            }
        }

        if (sentCount > 0) {
            // Updated log message
            logger.trace("Broadcasted OrderStatus update for orderId {} to {} sessions", orderId, sentCount);
        } else {
            logger.trace("No active sessions to broadcast OrderStatus update for orderId {} to.", orderId);
        }
    }
}
