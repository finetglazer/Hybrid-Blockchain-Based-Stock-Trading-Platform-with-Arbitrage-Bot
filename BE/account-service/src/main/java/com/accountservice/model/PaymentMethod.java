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
    /**
     * <p>
     * The following key-value pairs are being added to the metadata map:
     * <p>
     * - "accountHolderName": [String] The full name of the individual or entity holding the bank account.
     *                        Value sourced from `detailsRequest.getAccountHolderName()`.
     * <p>
     * - "routingNumber":     [String] The ABA routing transit number identifying the bank.
     *                        Value sourced from `detailsRequest.getRoutingNumber()`.
     * <p>
     * - "bankName":          [String] The commonly known name of the financial institution.
     *                        Value sourced from `detailsRequest.getBankName()`.
     * <p>
     * - "accountNumber":     [String] The specific bank account number associated with this payment method.
     *                        NOTE: Consider security implications of storing the full number here vs. masked/tokenized.
     *                        Value sourced from `detailsRequest.getAccountNumber()`.
     * <p>
     * - "verificationRequired": [Boolean] A flag explicitly set to `true`, indicating that this
     *                           bank account requires a verification step (like micro-deposit validation)
     *                           before it can be considered fully active or used for certain transactions.
     * <p>
     * - "verificationMethod": [String] Specifies the exact method to be used for bank account
     *                           verification. Set to `"MICRO_DEPOSITS"`, implying that small, random
     *                           deposits will be made to the account, which the user must then confirm.
     * <p>
     * - "verifiedAt":        [Object, typically Timestamp/Date/Instant, or null] Stores the date and time
     *                        when the bank account verification was successfully completed. It is initialized
     *                        to `null` here, signifying that the verification process has not yet been
     *                        successfully completed for this payment method.
     */
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
