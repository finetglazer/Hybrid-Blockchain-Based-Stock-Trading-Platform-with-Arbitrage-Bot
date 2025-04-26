package com.stocktrading.marketdata.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktrading.marketdata.model.StockUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


@Component
public class StockDataWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(StockDataWebSocketHandler.class);

    // Thread-safe set to keep track of all active sessions
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket connection established: {}, Total sessions: {}", session.getId(), sessions.size());
        // Optional: Send a welcome message or initial state if needed
        // session.sendMessage(new TextMessage("{\"status\": \"connected\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session); // Ensure removal on error
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} with status {}, Total sessions: {}", session.getId(), status, sessions.size());
    }

    /**
     * Sends a stock update (received from Kafka) to ALL connected WebSocket clients.
     *
     * @param update The StockUpdate data to send.
     */
    public void broadcastStockUpdate(StockUpdate update) {
        if (update == null) return;

        TextMessage message;
        try {
            // Convert the StockUpdate object to a JSON string
            message = new TextMessage(objectMapper.writeValueAsString(update));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize StockUpdate for symbol {}: {}", update.getSymbol(), e.getMessage());
            return;
        }

        // Iterate over a snapshot of the sessions to avoid issues if a session closes during iteration
        // CopyOnWriteArraySet's iterator is weakly consistent and safe here.
        int sentCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    // Send the JSON message to the client
                    session.sendMessage(message);
                    sentCount++;
                } catch (IOException e) {
                    logger.error("Failed to send message to session {}. Error: {}", session.getId(), e.getMessage());
                    // Consider removing the session if sending repeatedly fails,
                    // though afterConnectionClosed should handle most cases.
                     sessions.remove(session); // Be careful with ConcurrentModificationException if not using CopyOnWriteArraySet
                }
            } else {
                // Optional: Proactively remove sessions found closed during broadcast
                 logger.debug("Removing closed session found during broadcast: {}", session.getId());
                 sessions.remove(session);
            }
        }
        if (sentCount > 0) {
            logger.trace("Broadcasted update for {} to {} sessions", update.getSymbol(), sentCount);
        }
    }
}
