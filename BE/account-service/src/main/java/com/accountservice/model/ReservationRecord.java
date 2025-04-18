package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservation_records")
public class ReservationRecord {
    @Id
    private String id;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private String referenceId;
    private Instant createdAt;
    private Instant expiresAt;
    private String status;
    private Instant updatedAt;

    public enum ReservationStatus {
        ACTIVE,
        PARTIALLY_RELEASED,
        FULLY_RELEASED,
        SETTLED,
        EXPIRED,
        REVERSED  // Add this new status
    }
}