package com.stocktrading.portfolio.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.portfolio.model.Portfolio;
import com.stocktrading.portfolio.model.Position;
import com.stocktrading.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final PortfolioRepository portfolioRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.portfolio-events}")
    private String portfolioEventsTopic;

    /**
     * Handle PORTFOLIO_UPDATE_POSITIONS command
     */
    public void handleUpdatePositions(CommandMessage command) {
        log.info("Handling PORTFOLIO_UPDATE_POSITIONS command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        Integer quantity = command.getPayloadValue("quantity");
        Object priceObj = command.getPayloadValue("price");
        BigDecimal price = convertToBigDecimal(priceObj);
        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PORTFOLIO_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Get or create portfolio
            Portfolio portfolio = getOrCreatePortfolio(userId, accountId);

            // Create new position
            Position position = Position.builder()
                    .stockSymbol(stockSymbol)
                    .quantity(quantity)
                    .averagePrice(price)
                    .currentPrice(price) // Initially set current price to purchase price
                    .acquiredAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            // Add position to portfolio
            portfolio.addPosition(position);
            portfolio.setUpdatedAt(Instant.now());

            // Save updated portfolio
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

            // Set success response
            event.setType("POSITIONS_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("portfolioId", updatedPortfolio.getId());
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("quantity", quantity);
            event.setPayloadValue("price", price);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("updatedAt", updatedPortfolio.getUpdatedAt().toString());

            log.info("Portfolio positions updated successfully for user: {}, account: {}, stock: {}",
                    userId, accountId, stockSymbol);

        } catch (Exception e) {
            log.error("Error updating portfolio positions", e);
            handleUpdateFailure(event, "POSITIONS_UPDATE_ERROR",
                    "Error updating portfolio positions: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioEventsTopic, command.getSagaId(), event);
            log.info("Sent POSITIONS_UPDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle PORTFOLIO_REMOVE_POSITIONS command (compensation)
     */
    public void handleRemovePositions(CommandMessage command) {
        log.info("Handling PORTFOLIO_REMOVE_POSITIONS command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        Integer quantity = command.getPayloadValue("quantity");
        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PORTFOLIO_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find portfolio
            var portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

            // If portfolio doesn't exist or no positions to remove, return success (idempotency)
            if (portfolioOpt.isEmpty()) {
                log.info("Portfolio not found for removal. User: {}, Account: {}", userId, accountId);
                event.setType("POSITIONS_REMOVED");
                event.setSuccess(true);
                event.setPayloadValue("userId", userId);
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("quantity", quantity);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("status", "NO_PORTFOLIO");

                kafkaTemplate.send(portfolioEventsTopic, command.getSagaId(), event);
                return;
            }

            Portfolio portfolio = portfolioOpt.get();

            // Remove positions
            boolean removed = portfolio.removePositionQuantity(stockSymbol, quantity);
            portfolio.setUpdatedAt(Instant.now());

            // Save updated portfolio
            portfolioRepository.save(portfolio);

            // Set success response
            event.setType("POSITIONS_REMOVED");
            event.setSuccess(true);
            event.setPayloadValue("portfolioId", portfolio.getId());
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("quantity", quantity);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("status", removed ? "REMOVED" : "NOT_FOUND");
            event.setPayloadValue("updatedAt", portfolio.getUpdatedAt().toString());

            log.info("Portfolio positions removed successfully for user: {}, account: {}, stock: {}",
                    userId, accountId, stockSymbol);

        } catch (Exception e) {
            log.error("Error removing portfolio positions", e);
            event.setType("POSITIONS_REMOVAL_FAILED");
            event.setSuccess(false);
            event.setErrorCode("POSITIONS_REMOVAL_ERROR");
            event.setErrorMessage("Error removing portfolio positions: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioEventsTopic, command.getSagaId(), event);
            log.info("Sent positions removal response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle position update failures
     */
    private void handleUpdateFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("POSITIONS_UPDATE_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(portfolioEventsTopic, event.getSagaId(), event);
            log.info("Sent POSITIONS_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method to get or create a portfolio
     */
    private Portfolio getOrCreatePortfolio(String userId, String accountId) {
        // Try to find existing portfolio
        var portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

        if (portfolioOpt.isPresent()) {
            return portfolioOpt.get();
        }

        // Create new portfolio if not found
        Portfolio newPortfolio = Portfolio.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .accountId(accountId)
                .name("Default Portfolio")
                .positions(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return portfolioRepository.save(newPortfolio);
    }

    /**
     * Helper method to safely convert any numeric type to BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object amountObj) {
        if (amountObj instanceof BigDecimal) {
            return (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            return BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        } else if (amountObj == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
    }
}