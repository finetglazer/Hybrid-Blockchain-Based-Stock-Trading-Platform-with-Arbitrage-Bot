package com.accountservice.service.kafka;

import com.accountservice.model.Balance;
import com.accountservice.model.PaymentMethod;
import com.accountservice.model.TradingAccount;
import com.accountservice.model.Transaction;
import com.accountservice.repository.BalanceRepository;
import com.accountservice.repository.PaymentMethodRepository;
import com.accountservice.repository.TradingAccountRepository;
import com.accountservice.repository.TransactionRepository;
import com.accountservice.service.PaymentMethodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final PaymentMethodService paymentMethodService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;

    /**
     * Handle ACCOUNT_VALIDATE command
     */
    public void handleAccountValidation(CommandMessage command) {
        log.info("Handling ACCOUNT_VALIDATE command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String userId = command.getPayloadValue("userId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Validate account exists
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleAccountValidationFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Validate account belongs to user
            if (!account.getUserId().equals(userId)) {
                handleAccountValidationFailure(event, "ACCOUNT_NOT_AUTHORIZED",
                        "Account does not belong to user");
                return;
            }

            // Check if account is active
            if (!account.getStatus().equals("ACTIVE")) {
                handleAccountValidationFailure(event, "ACCOUNT_NOT_ACTIVE",
                        "Account is not active: " + account.getStatus());
                return;
            }

            // All validations passed
            event.setType("ACCOUNT_VALIDATED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("accountStatus", account.getStatus());

        } catch (Exception e) {
            log.error("Error validating account", e);
            handleAccountValidationFailure(event, "VALIDATION_ERROR",
                    "Error validating account: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent ACCOUNT_VALIDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle account validation failures
     */
    private void handleAccountValidationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("ACCOUNT_VALIDATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent ACCOUNT_VALIDATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle PAYMENT_METHOD_VALIDATE command
     */
    public void handleValidatePaymentMethod(CommandMessage command) {
        log.info("Handling PAYMENT_METHOD_VALIDATE command for saga: {}", command.getSagaId());

        String paymentMethodId = command.getPayloadValue("paymentMethodId");
        String userId = command.getPayloadValue("userId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Validate the payment method
            PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);

            // Check if payment method exists and belongs to user
            if (paymentMethod == null) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_FOUND",
                        "Payment method not found: " + paymentMethodId);
                return;
            }

            if (!paymentMethod.getUserId().equals(userId)) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_AUTHORIZED",
                        "Payment method does not belong to user");
                return;
            }

            // Check status
            if (!paymentMethod.getStatus().equals(PaymentMethod.PaymentMethodStatus.ACTIVE.toString())) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_ACTIVE",
                        "Payment method is not active: " + paymentMethod.getStatus());
                return;
            }

            // All validations passed
            event.setType("PAYMENT_METHOD_VALID");
            event.setSuccess(true);
            event.setPayloadValue("paymentMethodId", paymentMethodId);
            event.setPayloadValue("paymentMethodType", paymentMethod.getType().toString());
            event.setPayloadValue("paymentMethodName", paymentMethod.getNickname());

        } catch (Exception e) {
            log.error("Error validating payment method", e);
            handleValidationFailure(event, "VALIDATION_ERROR",
                    "Error validating payment method: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent PAYMENT_METHOD_VALID response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle validation failures
     */
    private void handleValidationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("PAYMENT_METHOD_INVALID");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent PAYMENT_METHOD_INVALID response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_CREATE_PENDING_TRANSACTION command
     */
    public void handleCreatePendingTransaction(CommandMessage command) {
        log.info("Handling ACCOUNT_CREATE_PENDING_TRANSACTION command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        // Safe conversion from any numeric type to BigDecimal
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount;
        if (amountObj instanceof BigDecimal) {
            amount = (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            amount = new BigDecimal((String) amountObj);
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
        String currency = command.getPayloadValue("currency");
        String description = command.getPayloadValue("description");
        String paymentMethodId = command.getPayloadValue("paymentMethodId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // No need to validate account here - it's already validated in the previous step

            // Create pending transaction
            Transaction transaction = new Transaction();
            transaction.setId(UUID.randomUUID().toString());
            transaction.setAccountId(accountId);
            transaction.setType("DEPOSIT"); // Using String type since we have a String field, not enum
            transaction.setStatus("PENDING"); // Using String status
            transaction.setAmount(amount);
            transaction.setCurrency(currency);
            transaction.setFee(BigDecimal.ZERO); // Set fee later if needed
            transaction.setDescription(description);
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            transaction.setPaymentMethodId(paymentMethodId);

            // Debug output
            log.debug("Saving transaction: {}", transaction);
            log.debug("Amount class: {}, Fee class: {}",
                    transaction.getAmount().getClass().getName(),
                    transaction.getFee().getClass().getName());

            // Save the transaction
            Transaction savedTransaction = transactionRepository.save(transaction);

            // Set success response
            event.setType("TRANSACTION_CREATED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", savedTransaction.getId());
            event.setPayloadValue("status", savedTransaction.getStatus());
            event.setPayloadValue("amount", savedTransaction.getAmount());
            event.setPayloadValue("currency", savedTransaction.getCurrency());
            event.setPayloadValue("createdAt", savedTransaction.getCreatedAt().toString());

        } catch (Exception e) {
            log.error("Error creating pending transaction", e);
            handleTransactionFailure(event, "TRANSACTION_CREATION_ERROR",
                    "Error creating pending transaction: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent TRANSACTION_CREATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle transaction creation failures
     */
    private void handleTransactionFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("TRANSACTION_CREATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent TRANSACTION_CREATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_UPDATE_TRANSACTION_STATUS command
     */
    public void handleUpdateTransactionStatus(CommandMessage command) {
        log.info("Handling ACCOUNT_UPDATE_TRANSACTION_STATUS command for saga: {}", command.getSagaId());

        String transactionId = command.getPayloadValue("transactionId");
        String status = command.getPayloadValue("status");
        String paymentReference = command.getPayloadValue("paymentReference");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                handleTransactionUpdateFailure(event, "TRANSACTION_NOT_FOUND",
                        "Transaction not found: " + transactionId);
                return;
            }

            Transaction transaction = transactionOpt.get();

            // Update transaction
            transaction.setStatus(status);
            transaction.setCompletedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            transaction.setExternalReferenceId(paymentReference);

            log.debug("Updating transaction status to: {}", status);
            Transaction updatedTransaction = transactionRepository.save(transaction);

            // Set success response
            event.setType("TRANSACTION_STATUS_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", updatedTransaction.getId());
            event.setPayloadValue("status", updatedTransaction.getStatus());
            event.setPayloadValue("accountId", updatedTransaction.getAccountId());
            event.setPayloadValue("completedAt", updatedTransaction.getCompletedAt().toString());

        } catch (Exception e) {
            log.error("Error updating transaction status", e);
            handleTransactionUpdateFailure(event, "TRANSACTION_UPDATE_ERROR",
                    "Error updating transaction status: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent TRANSACTION_STATUS_UPDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle transaction update failures
     */
    private void handleTransactionUpdateFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("TRANSACTION_UPDATE_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent TRANSACTION_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_UPDATE_BALANCE command
     */
    public void handleUpdateBalance(CommandMessage command) {
        log.info("Handling ACCOUNT_UPDATE_BALANCE command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        // Safe conversion from any numeric type to BigDecimal
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount;
        if (amountObj instanceof BigDecimal) {
            amount = (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            amount = new BigDecimal((String) amountObj);
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
        String transactionId = command.getPayloadValue("transactionId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleBalanceUpdateFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find or create balance
            Balance balance = getOrCreateBalance(account);

            // Update balance
            BigDecimal newAvailable = balance.getAvailable().add(amount);
            BigDecimal newTotal = balance.getTotal().add(amount);

            balance.setAvailable(newAvailable);
            balance.setTotal(newTotal);
            balance.setUpdatedAt(Instant.now());

            log.debug("Updating balance: available={}, total={}", newAvailable, newTotal);

            // Save the updated balance
            balanceRepository.save(balance);

            // Set success response
            event.setType("BALANCE_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("transactionId", transactionId);
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("updateType", "DEPOSIT");
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());

        } catch (Exception e) {
            log.error("Error updating balance", e);
            handleBalanceUpdateFailure(event, "BALANCE_UPDATE_ERROR",
                    "Error updating balance: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent BALANCE_UPDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle balance update failures
     */
    private void handleBalanceUpdateFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("BALANCE_UPDATE_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent BALANCE_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method to get or create a balance for an account
     */
    private Balance getOrCreateBalance(TradingAccount account) {
        // First try to find existing balance
        Balance balance = balanceRepository.findByAccountId(account.getId());

        // If no balance exists, create a new one
        if (balance == null) {
            balance = new Balance();
            balance.setId(UUID.randomUUID().toString());
            balance.setAccountId(account.getId());
            balance.setCurrency("USD"); // Default currency
            balance.setAvailable(BigDecimal.ZERO);
            balance.setReserved(BigDecimal.ZERO);
            balance.setTotal(BigDecimal.ZERO);
            balance.setUpdatedAt(Instant.now());
        }

        return balance;
    }

    // Add these methods to KafkaCommandHandlerService in the Account Service

    /**
     * Handle ACCOUNT_MARK_TRANSACTION_FAILED command
     */
    public void handleMarkTransactionFailed(CommandMessage command) {
        log.info("Handling ACCOUNT_MARK_TRANSACTION_FAILED command for saga: {}", command.getSagaId());

        String transactionId = command.getPayloadValue("transactionId");
        String failureReason = command.getPayloadValue("failureReason");
        String errorCode = command.getPayloadValue("errorCode");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                handleTransactionFailureError(event, "TRANSACTION_NOT_FOUND",
                        "Transaction not found: " + transactionId);
                return;
            }

            Transaction transaction = transactionOpt.get();

            // Update transaction to FAILED status
            transaction.setStatus("FAILED");
            transaction.setUpdatedAt(Instant.now());
            transaction.setDescription(transaction.getDescription() + " - FAILED: " + failureReason);

            log.debug("Marking transaction as failed: {}", transactionId);
            Transaction updatedTransaction = transactionRepository.save(transaction);

            // Set success response
            event.setType("TRANSACTION_MARKED_FAILED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", updatedTransaction.getId());
            event.setPayloadValue("status", updatedTransaction.getStatus());
            event.setPayloadValue("accountId", updatedTransaction.getAccountId());
            event.setPayloadValue("updatedAt", updatedTransaction.getUpdatedAt().toString());

        } catch (Exception e) {
            log.error("Error marking transaction as failed", e);
            handleTransactionFailureError(event, "TRANSACTION_UPDATE_ERROR",
                    "Error marking transaction as failed: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent TRANSACTION_MARKED_FAILED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method for transaction failure errors
     */
    private void handleTransactionFailureError(EventMessage event, String errorCode, String errorMessage) {
        event.setType("TRANSACTION_MARK_FAILED_ERROR");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent TRANSACTION_MARK_FAILED_ERROR response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_REVERSE_BALANCE_UPDATE command
     */
    public void handleReverseBalanceUpdate(CommandMessage command) {
        log.info("Handling ACCOUNT_REVERSE_BALANCE_UPDATE command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        // Safe conversion from any numeric type to BigDecimal
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount;
        if (amountObj instanceof BigDecimal) {
            amount = (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            amount = new BigDecimal((String) amountObj);
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
        String transactionId = command.getPayloadValue("transactionId");
        String reason = command.getPayloadValue("reason");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleBalanceReverseFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            // Find balance
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                handleBalanceReverseFailure(event, "BALANCE_NOT_FOUND",
                        "Balance not found for account: " + accountId);
                return;
            }

            // Reverse the balance update (subtract the amount since it was a deposit)
            BigDecimal newAvailable = balance.getAvailable().subtract(amount);
            BigDecimal newTotal = balance.getTotal().subtract(amount);

            balance.setAvailable(newAvailable);
            balance.setTotal(newTotal);
            balance.setUpdatedAt(Instant.now());

            log.debug("Reversing balance update: available={}, total={}", newAvailable, newTotal);

            // Save the updated balance
            balanceRepository.save(balance);

            // Create a reversal transaction record
            Transaction reversalTransaction = new Transaction();
            reversalTransaction.setId(UUID.randomUUID().toString());
            reversalTransaction.setAccountId(accountId);
            reversalTransaction.setType("DEPOSIT_REVERSAL");
            reversalTransaction.setStatus("COMPLETED");
            reversalTransaction.setAmount(amount.negate()); // Negative amount
            reversalTransaction.setCurrency(balance.getCurrency());
            reversalTransaction.setFee(BigDecimal.ZERO);
            reversalTransaction.setDescription("Reversal of deposit due to: " + reason);
            reversalTransaction.setCreatedAt(Instant.now());
            reversalTransaction.setUpdatedAt(Instant.now());
            reversalTransaction.setCompletedAt(Instant.now());
            reversalTransaction.setExternalReferenceId("REV-" + transactionId);

            transactionRepository.save(reversalTransaction);

            // Set success response
            event.setType("BALANCE_REVERSAL_COMPLETED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("originalTransactionId", transactionId);
            event.setPayloadValue("reversalTransactionId", reversalTransaction.getId());
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("reversedAmount", amount);
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());

        } catch (Exception e) {
            log.error("Error reversing balance update", e);
            handleBalanceReverseFailure(event, "BALANCE_REVERSAL_ERROR",
                    "Error reversing balance update: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent BALANCE_REVERSAL_COMPLETED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method for balance reversal failures
     */
    private void handleBalanceReverseFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("BALANCE_REVERSAL_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent BALANCE_REVERSAL_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
}