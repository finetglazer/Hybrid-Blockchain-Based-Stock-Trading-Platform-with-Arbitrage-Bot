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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final PortfolioRepository portfolioRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.portfolio-events}")
    private String portfolioEventsTopic;

    @Value("${kafka.topics.portfolio-events.order-sell}")
    private String portfolioOrderSellEventsTopic;

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
     * Handle PORTFOLIO_VERIFY_SHARES command for order sell saga
     */
    public void handleVerifyShares(CommandMessage command) {
        log.info("Handling PORTFOLIO_VERIFY_SHARES command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        Integer quantity = command.getPayloadValue("quantity");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PORTFOLIO_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find portfolio
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

            // Check if portfolio exists
            if (portfolioOpt.isEmpty()) {
                // Portfolio not found
                event.setType("SHARES_VALIDATION_FAILED");
                event.setSuccess(false);
                event.setErrorCode("PORTFOLIO_NOT_FOUND");
                event.setErrorMessage("Portfolio not found for user: " + userId + ", account: " + accountId);

                kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
                log.warn("Portfolio not found for share verification. User: {}, Account: {}", userId, accountId);
                return;
            }

            Portfolio portfolio = portfolioOpt.get();
            boolean hasSufficientShares = false;
            int availableQuantity = 0;

            // Check if position exists for the given stock
            if (portfolio.getPositions() != null) {
                for (Position position : portfolio.getPositions()) {
                    if (position.getStockSymbol().equals(stockSymbol)) {
                        availableQuantity = position.getQuantity();
                        hasSufficientShares = position.getQuantity() >= quantity;
                        break;
                    }
                }
            }

            if (hasSufficientShares) {
                // Sufficient shares available
                event.setType("SHARES_VALIDATED");
                event.setSuccess(true);
                event.setPayloadValue("userId", userId);
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("requestedQuantity", quantity);
                event.setPayloadValue("availableQuantity", availableQuantity);

                log.info("Sufficient shares verified for user: {}, account: {}, stock: {}, quantity: {}",
                        userId, accountId, stockSymbol, quantity);
            } else {
                // Insufficient shares
                event.setType("SHARES_VALIDATION_FAILED");
                event.setSuccess(false);
                event.setErrorCode("INSUFFICIENT_SHARES");
                event.setErrorMessage("Insufficient shares for stock: " + stockSymbol +
                        ". Required: " + quantity + ", Available: " + availableQuantity);

                log.warn("Insufficient shares for user: {}, account: {}, stock: {}, requested: {}, available: {}",
                        userId, accountId, stockSymbol, quantity, availableQuantity);
            }

        } catch (Exception e) {
            log.error("Error verifying shares", e);
            event.setType("SHARES_VALIDATION_FAILED");
            event.setSuccess(false);
            event.setErrorCode("VERIFICATION_ERROR");
            event.setErrorMessage("Error verifying shares: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent share verification response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle PORTFOLIO_RESERVE_SHARES command for order sell saga
     */
    public void handleReserveShares(CommandMessage command) {
        log.info("Handling PORTFOLIO_RESERVE_SHARES command for saga: {}", command.getSagaId());

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
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

            if (portfolioOpt.isEmpty()) {
                event.setType("SHARES_RESERVATION_FAILED");
                event.setSuccess(false);
                event.setErrorCode("PORTFOLIO_NOT_FOUND");
                event.setErrorMessage("Portfolio not found for user: " + userId + ", account: " + accountId);

                kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            Portfolio portfolio = portfolioOpt.get();
            boolean sufficientShares = false;
            Position targetPosition = null;

            // Check if position exists with sufficient shares
            if (portfolio.getPositions() != null) {
                for (Position position : portfolio.getPositions()) {
                    if (position.getStockSymbol().equals(stockSymbol) && position.getQuantity() >= quantity) {
                        sufficientShares = true;
                        targetPosition = position;
                        break;
                    }
                }
            }

            if (!sufficientShares || targetPosition == null) {
                event.setType("SHARES_RESERVATION_FAILED");
                event.setSuccess(false);
                event.setErrorCode("INSUFFICIENT_SHARES");
                event.setErrorMessage("Insufficient shares for reservation");

                kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            // Generate reservation ID
            String reservationId = UUID.randomUUID().toString();

            // Store reservation in a map (in a real application, this would be stored in a database)
            // This is a simplified implementation - in production, you'd use a proper reservation system
            Map<String, Object> reservationData = new HashMap<>();
            reservationData.put("reservationId", reservationId);
            reservationData.put("userId", userId);
            reservationData.put("accountId", accountId);
            reservationData.put("stockSymbol", stockSymbol);
            reservationData.put("quantity", quantity);
            reservationData.put("orderId", orderId);
            reservationData.put("timestamp", Instant.now());

            // In a real implementation, save the reservation in the database
            // For now, we'll just log it
            log.info("Created share reservation: {}", reservationData);

            // Success event
            event.setType("SHARES_RESERVED");
            event.setSuccess(true);
            event.setPayloadValue("reservationId", reservationId);
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("reservedQuantity", quantity);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("currentPrice", targetPosition.getCurrentPrice());

            log.info("Shares reserved successfully for user: {}, account: {}, stock: {}, quantity: {}",
                    userId, accountId, stockSymbol, quantity);

        } catch (Exception e) {
            log.error("Error reserving shares", e);
            event.setType("SHARES_RESERVATION_FAILED");
            event.setSuccess(false);
            event.setErrorCode("RESERVATION_ERROR");
            event.setErrorMessage("Error reserving shares: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent share reservation response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle PORTFOLIO_RELEASE_SHARES command for order sell saga
     */
    public void handleReleaseShares(CommandMessage command) {
        log.info("Handling PORTFOLIO_RELEASE_SHARES command for saga: {}", command.getSagaId());

        String reservationId = command.getPayloadValue("reservationId");
        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PORTFOLIO_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // In a real implementation, look up the reservation in the database and release it
            // For now, we'll just log it
            log.info("Released share reservation: {}", reservationId);

            // Success event
            event.setType("SHARES_RELEASED");
            event.setSuccess(true);
            event.setPayloadValue("reservationId", reservationId);
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("orderId", orderId);

            log.info("Shares released successfully for reservation: {}, user: {}, account: {}, stock: {}",
                    reservationId, userId, accountId, stockSymbol);

        } catch (Exception e) {
            log.error("Error releasing shares", e);
            event.setType("SHARES_RELEASE_FAILED");
            event.setSuccess(false);
            event.setErrorCode("RELEASE_ERROR");
            event.setErrorMessage("Error releasing shares: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent share release response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle PORTFOLIO_RESTORE_POSITIONS command for order sell saga compensation
     */
    public void handleRestorePositions(CommandMessage command) {
        log.info("Handling PORTFOLIO_RESTORE_POSITIONS command for saga: {}", command.getSagaId());

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
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

            if (portfolioOpt.isEmpty()) {
                // Portfolio not found, create a new one with the restored position
                Portfolio newPortfolio = createPortfolioWithPosition(userId, accountId, stockSymbol, quantity);
                portfolioRepository.save(newPortfolio);

                event.setType("PORTFOLIO_POSITIONS_RESTORED");
                event.setSuccess(true);
                event.setPayloadValue("portfolioId", newPortfolio.getId());
                event.setPayloadValue("userId", userId);
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("quantity", quantity);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("status", "NEW_PORTFOLIO_CREATED");
            } else {
                // Portfolio exists, add or update position
                Portfolio portfolio = portfolioOpt.get();

                // Find existing position
                Position existingPosition = null;
                if (portfolio.getPositions() != null) {
                    for (Position position : portfolio.getPositions()) {
                        if (position.getStockSymbol().equals(stockSymbol)) {
                            existingPosition = position;
                            break;
                        }
                    }
                }

                if (existingPosition != null) {
                    // Update existing position
                    existingPosition.setQuantity(existingPosition.getQuantity() + quantity);
                    existingPosition.setUpdatedAt(Instant.now());
                } else {
                    // Create new position
                    Position newPosition = Position.builder()
                            .stockSymbol(stockSymbol)
                            .quantity(quantity)
                            .averagePrice(BigDecimal.ZERO) // Would typically be retrieved from execution price
                            .currentPrice(BigDecimal.ZERO) // Would typically be retrieved from market data
                            .acquiredAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    if (portfolio.getPositions() == null) {
                        portfolio.setPositions(new ArrayList<>());
                    }
                    portfolio.getPositions().add(newPosition);
                }

                portfolio.setUpdatedAt(Instant.now());
                portfolioRepository.save(portfolio);

                event.setType("PORTFOLIO_POSITIONS_RESTORED");
                event.setSuccess(true);
                event.setPayloadValue("portfolioId", portfolio.getId());
                event.setPayloadValue("userId", userId);
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("quantity", quantity);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("status", "POSITION_RESTORED");
            }

            log.info("Portfolio positions restored for user: {}, account: {}, stock: {}, quantity: {}",
                    userId, accountId, stockSymbol, quantity);

        } catch (Exception e) {
            log.error("Error restoring portfolio positions", e);
            event.setType("PORTFOLIO_POSITIONS_RESTORE_FAILED");
            event.setSuccess(false);
            event.setErrorCode("RESTORE_ERROR");
            event.setErrorMessage("Error restoring portfolio positions: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent position restore response for saga: {}", command.getSagaId());
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
     * Helper method to create a portfolio with a single position
     */
    private Portfolio createPortfolioWithPosition(String userId, String accountId, String stockSymbol, Integer quantity) {
        Position position = Position.builder()
                .stockSymbol(stockSymbol)
                .quantity(quantity)
                .averagePrice(BigDecimal.ZERO)
                .currentPrice(BigDecimal.ZERO)
                .acquiredAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        List<Position> positions = new ArrayList<>();
        positions.add(position);

        return Portfolio.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .accountId(accountId)
                .name("Default Portfolio")
                .positions(positions)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }


    /**
     * Handle PORTFOLIO_UPDATE_POSITIONS command for order sell saga
     * This permanently removes the shares after a successful sell order
     */
    public void handleUpdatePortfolioForSellOrder(CommandMessage command) {
        log.info("Handling PORTFOLIO_UPDATE_POSITIONS for sell order, saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        Integer quantity = command.getPayloadValue("quantity");
        String orderId = command.getPayloadValue("orderId");
        String reservationId = command.getPayloadValue("reservationId");
        Object priceObj = command.getPayloadValue("price");
        BigDecimal executionPrice = convertToBigDecimal(priceObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PORTFOLIO_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find portfolio
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdAndAccountId(userId, accountId);

            if (portfolioOpt.isEmpty()) {
                // Portfolio not found - this is an error since we should have a portfolio at this point
                event.setType("POSITIONS_UPDATE_FAILED");
                event.setSuccess(false);
                event.setErrorCode("PORTFOLIO_NOT_FOUND");
                event.setErrorMessage("Portfolio not found for user: " + userId + ", account: " + accountId);

                kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            Portfolio portfolio = portfolioOpt.get();

            // In a real implementation, we would verify and clear the reservation first
            // Since we only mocked the reservation, we skip that step here
            log.info("Releasing reservation and removing shares: {}", reservationId);

            // Remove the shares from the portfolio
            boolean removed = portfolio.removePositionQuantity(stockSymbol, -quantity);

            if (!removed) {
                event.setType("POSITIONS_UPDATE_FAILED");
                event.setSuccess(false);
                event.setErrorCode("SHARES_NOT_FOUND");
                event.setErrorMessage("Could not find sufficient shares to remove for stock: " + stockSymbol);

                kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            // Update the portfolio with the timestamp
            portfolio.setUpdatedAt(Instant.now());
            portfolioRepository.save(portfolio);

            // Calculate realized gain/loss if needed
            // For this we'd need the original purchase price information
            // BigDecimal realizedPL = executionPrice.subtract(averageBuyPrice).multiply(new BigDecimal(quantity));

            // Send success response
            event.setType("POSITIONS_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("portfolioId", portfolio.getId());
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("quantity", quantity);
            event.setPayloadValue("executionPrice", executionPrice);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("updatedAt", portfolio.getUpdatedAt().toString());

            log.info("Portfolio positions updated for sell order, user: {}, account: {}, stock: {}, quantity: {}",
                    userId, accountId, stockSymbol, quantity);

        } catch (Exception e) {
            log.error("Error updating portfolio positions for sell order", e);
            event.setType("POSITIONS_UPDATE_FAILED");
            event.setSuccess(false);
            event.setErrorCode("UPDATE_ERROR");
            event.setErrorMessage("Error updating portfolio positions: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(portfolioOrderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent position update response for sell order saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
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