package com.stocktrading.marketdata.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.marketdata.model.StockUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
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
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
        sessions.remove(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} with status {}, Total sessions: {}",
                session.getId(), status, sessions.size());
    }

    /**
     * Receives EventMessage from Kafka, converts it to StockUpdate, and broadcasts to WebSocket clients
     */
    @KafkaListener(
            topics = "${kafka.topics.market-price-updates}",
            containerFactory = "eventMessageListenerContainerFactory",
            groupId = "stock-updates-consumer-group"
    )
    public void broadcastStockUpdate(EventMessage eventMessage, Acknowledgment ack) {
        try {
            if (eventMessage == null || !eventMessage.getSuccess()) {
                logger.warn("Received null or unsuccessful event message");
                ack.acknowledge();
                return;
            }

            // Extract data from EventMessage payload
            Map<String, Object> payload = eventMessage.getPayload();
            if (payload == null) {
                logger.warn("Event message has null payload");
                ack.acknowledge();
                return;
            }

            // Convert EventMessage to StockUpdate
            StockUpdate update = new StockUpdate();
            update.setSymbol(getStringValue(payload, "symbol"));
            update.setPrice(getBigDecimalValue(payload, "price"));
            update.setBidPrice(getBigDecimalValue(payload, "bidPrice"));
            update.setAskPrice(getBigDecimalValue(payload, "askPrice"));
            update.setVolume(getLongValue(payload, "volume"));
            update.setTimestamp(getStringValue(payload, "timestamp"));
            update.setChange(getBigDecimalValue(payload, "change"));
            update.setChangePercent(getBigDecimalValue(payload, "changePercent"));

            // Set company name based on symbol (using the same logic as in your existing code)
            update.setCompany(getCompanyName(update.getSymbol()));

            // Broadcast the update
            broadcastToClients(update);

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing stock update from EventMessage: {}", e.getMessage(), e);
            // Acknowledge despite error to prevent redelivery attempts for parsing issues
            ack.acknowledge();
        }
    }

    /**
     * Broadcasts the stock update to all connected WebSocket clients
     */
    private void broadcastToClients(StockUpdate update) {
        if (update == null || update.getSymbol() == null) {
            return;
        }

        TextMessage message;
        try {
            // Convert the StockUpdate object to a JSON string
            message = new TextMessage(objectMapper.writeValueAsString(update));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize StockUpdate for symbol {}: {}",
                    update.getSymbol(), e.getMessage());
            return;
        }

        int sentCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                    sentCount++;
                } catch (IOException e) {
                    logger.error("Failed to send message to session {}. Error: {}",
                            session.getId(), e.getMessage());
                    sessions.remove(session);
                }
            } else {
                logger.debug("Removing closed session found during broadcast: {}", session.getId());
                sessions.remove(session);
            }
        }

        if (sentCount > 0) {
            logger.trace("Broadcasted update for {} to {} sessions", update.getSymbol(), sentCount);
        }
    }

    /**
     * Helper method to get a company name from a stock symbol
     */
    private String getCompanyName(String symbol) {
        if (symbol == null) return "Unknown Company";

        switch (symbol) {
            case "AAPL": return "Apple Inc";
            case "MSFT": return "Microsoft Corporation";
            case "GOOGL": return "Alphabet Inc";
            case "AMZN": return "Amazon.com Inc";
            case "TSLA": return "Tesla Inc";
            case "META": return "Meta Platforms Inc";
            case "NVDA": return "NVIDIA Corporation";
            case "JPM": return "JPMorgan Chase & Co.";
            case "V": return "Visa Inc";
            case "JNJ": return "Johnson & Johnson";
            case "ABBV": return "AbbVie Inc.";
            case "WMT": return "Walmart Inc.";
            case "PG": return "Procter & Gamble Co.";
            case "MA": return "Mastercard Inc.";
            case "UNH": return "UnitedHealth Group Inc.";
            default: return symbol + " Corp";
        }
    }

    // Utility methods for safe extraction of values from the payload

    private String getStringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Could not convert {} to BigDecimal", value);
                return null;
            }
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Could not convert {} to Long", value);
                return null;
            }
        }
        return null;
    }
    /**
     * Processes an EventMessage and broadcasts it to clients
     * This method is used by KafkaEventListener for compatibility
     */
    public void processAndBroadcastEvent(EventMessage eventMessage) {
        try {
            if (eventMessage == null || !eventMessage.getSuccess()) {
                logger.warn("Received null or unsuccessful event message");
                return;
            }

            // Extract data from EventMessage payload
            Map<String, Object> payload = eventMessage.getPayload();
            if (payload == null) {
                logger.warn("Event message has null payload");
                return;
            }

            // Convert EventMessage to StockUpdate
            StockUpdate update = new StockUpdate();
            update.setSymbol(getStringValue(payload, "symbol"));
            update.setPrice(getBigDecimalValue(payload, "price"));
            update.setBidPrice(getBigDecimalValue(payload, "bidPrice"));
            update.setAskPrice(getBigDecimalValue(payload, "askPrice"));
            update.setVolume(getLongValue(payload, "volume"));
            update.setTimestamp(getStringValue(payload, "timestamp"));
            update.setChange(getBigDecimalValue(payload, "change"));
            update.setChangePercent(getBigDecimalValue(payload, "changePercent"));

            // Set company name based on symbol
            update.setCompany(getCompanyName(update.getSymbol()));

            // Broadcast the update
            broadcastToClients(update);

        } catch (Exception e) {
            logger.error("Error processing stock update from EventMessage: {}", e.getMessage(), e);
        }
    }
}