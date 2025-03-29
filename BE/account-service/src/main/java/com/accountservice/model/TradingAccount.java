package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "trading_accounts")
public class TradingAccount {
    @Id
    private String id;

    private String userId;
    private String nickname;
    private String accountNumber;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        RESTRICTED
    }
}
