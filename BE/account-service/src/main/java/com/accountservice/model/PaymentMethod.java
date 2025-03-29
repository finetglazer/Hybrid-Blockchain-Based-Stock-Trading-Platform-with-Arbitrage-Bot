package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "paymentMethods")
public class PaymentMethod {
    @Id
    private String id;

    private String userId;
    private String nickname;
    private Map<String, Object> metadata;
    private boolean isDefault;
    private String type;
    private String status;
    private String maskedNumber;
    private Instant addedAt;
    private Instant updatedAt;

    public enum PaymentMethodType {
        BANK_ACCOUNT,
        CREDIT_CARD,
        DEBIT_CARD,
        DIGITAL_WALLET
    }

    public enum PaymentMethodStatus {
        ACTIVE,
        INACTIVE,
        VERIFICATION_PENDING
    }
}
