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
@Document(collection = "balance")
public class Balance {
    @Id
    private String id;

    private String accountId;
    private BigDecimal total;
    private String currency;
    private BigDecimal available;
    private BigDecimal reserved;
    private Instant updatedAt;

    public void calculateTotal() {
        if (available == null) {
            available = BigDecimal.ZERO;
        }
        if (reserved == null) {
            reserved = BigDecimal.ZERO;
        }
        total = available.add(reserved);
    }
}
