package com.stocktrading.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "portfolios")
@CompoundIndex(name = "userId_accountId_idx", def = "{'userId': 1, 'accountId': 1}", unique = true)
public class Portfolio {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String accountId;

    private String name;
    private List<Position> positions;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Add position to the portfolio
     */
    public void addPosition(Position position) {
        if (positions == null) {
            positions = new ArrayList<>();
        }

        // Check if there's an existing position for this stock
        for (int i = 0; i < positions.size(); i++) {
            Position existingPosition = positions.get(i);
            if (existingPosition.getStockSymbol().equals(position.getStockSymbol())) {
                // Update existing position
                int newQuantity = existingPosition.getQuantity() + position.getQuantity();

                // Calculate new average price
                double totalInvested = existingPosition.getAveragePrice().doubleValue() * existingPosition.getQuantity() +
                        position.getAveragePrice().doubleValue() * position.getQuantity();
                double newAveragePrice = totalInvested / newQuantity;

                existingPosition.setQuantity(newQuantity);
                existingPosition.setAveragePrice(BigDecimal.valueOf(newAveragePrice));
                existingPosition.setUpdatedAt(Instant.now());
                return;
            }
        }

        // If no existing position, add new one
        positions.add(position);
    }

    /**
     * Remove position quantity from the portfolio
     */
    public boolean removePositionQuantity(String stockSymbol, int quantityToRemove) {
        if (positions == null) {
            return false;
        }

        for (int i = 0; i < positions.size(); i++) {
            Position existingPosition = positions.get(i);
            if (existingPosition.getStockSymbol().equals(stockSymbol)) {
                // Check if we have enough quantity
                if (existingPosition.getQuantity() < quantityToRemove) {
                    return false;
                }

                // Update position quantity
                int newQuantity = existingPosition.getQuantity() - quantityToRemove;
                if (newQuantity > 0) {
                    existingPosition.setQuantity(newQuantity);
                    existingPosition.setUpdatedAt(Instant.now());
                } else {
                    // Remove position entirely if quantity becomes zero
                    positions.remove(i);
                }
                return true;
            }
        }

        return false; // Position not found
    }
}