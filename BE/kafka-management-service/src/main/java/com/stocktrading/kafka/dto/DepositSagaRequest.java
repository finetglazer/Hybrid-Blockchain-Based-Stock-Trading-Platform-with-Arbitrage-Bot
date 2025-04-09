package com.stocktrading.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for starting a deposit saga
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositSagaRequest {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Account ID cannot be blank")
    private String accountId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @NotBlank(message = "Payment method ID cannot be blank")
    private String paymentMethodId;
}
