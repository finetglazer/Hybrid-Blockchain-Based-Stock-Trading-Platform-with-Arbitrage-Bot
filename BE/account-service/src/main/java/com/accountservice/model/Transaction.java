package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String accountId;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fee;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String paymentMethodId;
    private String externalReferenceId;

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        INTEREST,
        FEE,
        TRANSFER,
        ORDER_PAYMENT,
        REFUND
    }
}
