package com.stocktrading.marketdata.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final MarketPricePublisher marketPricePublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    private final List<String> trackedSymbols = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM", "V", "JNJ",
            "ABBV", "WMT", "PG", "MA", "UNH"
    );

    /**
     * Handle MARKET_VALIDATE_STOCK command
     */
    public void handleValidateStock(CommandMessage command, String responseTopic) {
        log.info("Handling MARKET_VALIDATE_STOCK command for saga: {}", command.getSagaId());

        String stockSymbol = command.getPayloadValue("stockSymbol");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("MARKET_DATA_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // check exist in trackedSymbols
            boolean stockExists = trackedSymbols.contains(stockSymbol);

            if (stockExists) {
                // Success case
                event.setType("STOCK_VALIDATED");
                event.setSuccess(true);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("isValid", true);
                log.info("Stock validated successfully: {}", stockSymbol);
            } else {
                // Stock doesn't exist
                handleValidationFailure(event, "STOCK_NOT_FOUND",
                        "Stock symbol not found: " + stockSymbol, responseTopic);
                return;
            }
        } catch (Exception e) {
            log.error("Error validating stock", e);
            handleValidationFailure(event, "VALIDATION_ERROR",
                    "Error validating stock: " + e.getMessage(), responseTopic);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(responseTopic, command.getSagaId(), event);
            log.info("Sent STOCK_VALIDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle MARKET_GET_PRICE command
     */
    public void handleGetPrice(CommandMessage command, String responseTopic) {
        log.info("Handling MARKET_GET_PRICE command for saga: {}", command.getSagaId());

        String stockSymbol = command.getPayloadValue("stockSymbol");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("MARKET_DATA_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Get current stock data from the market price publisher
            Map<String, Object> stockData = marketPricePublisher.getCurrentStockData(stockSymbol);

            if (stockData != null && stockData.containsKey("price")) {
                BigDecimal price = (BigDecimal) stockData.get("price");

                event.setType("PRICE_PROVIDED");
                event.setSuccess(true);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("currentPrice", price);
                event.setPayloadValue("timestamp", Instant.now().toString());

                log.info("Price provided for {}: ${}", stockSymbol, price);
            } else {
                // Failed to get price
                handlePriceFailure(event, "PRICE_RETRIEVAL_ERROR",
                        "Failed to retrieve price for: " + stockSymbol, responseTopic);
                return;
            }
        } catch (Exception e) {
            log.error("Error getting stock price", e);
            handlePriceFailure(event, "PRICE_RETRIEVAL_ERROR",
                    "Error getting stock price: " + e.getMessage(), responseTopic);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(responseTopic, command.getSagaId(), event);
            log.info("Sent PRICE_PROVIDED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method for stock validation failures
     */
    private void handleValidationFailure(EventMessage event, String errorCode, String errorMessage, String responseTopic) {
        event.setType("STOCK_VALIDATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(responseTopic, event.getSagaId(), event);
            log.info("Sent STOCK_VALIDATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method for price retrieval failures
     */
    private void handlePriceFailure(EventMessage event, String errorCode, String errorMessage, String responseTopic) {
        event.setType("PRICE_RETRIEVAL_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(responseTopic, event.getSagaId(), event);
            log.info("Sent PRICE_RETRIEVAL_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
}