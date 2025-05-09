package com.accountservice.service.kafka;

import com.accountservice.model.*;
import com.accountservice.repository.*;
import com.accountservice.service.PaymentMethodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
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
    private final ReservationRecordRepository reservationRecordRepository;
    private final Random random = new Random();

    @Value("${kafka.topics.account-events.common}")
    private String commonEventsTopic;

    @Value("${kafka.topics.account-events.deposit}")
    private String depositEventsTopic;

    @Value("${kafka.topics.account-events.withdrawal}")
    private String withdrawalEventsTopic;

    @Value("${kafka.topics.account-events.order-sell}")
    private String orderSellEventsTopic;

    /**
     * Handle ACCOUNT_VALIDATE command
     */
    public void handleAccountValidation(CommandMessage command) {
        log.info("Handling ACCOUNT_VALIDATE command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");

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
            kafkaTemplate.send(commonEventsTopic, command.getSagaId(), event);
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
            kafkaTemplate.send(commonEventsTopic, event.getSagaId(), event);
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
            kafkaTemplate.send(commonEventsTopic, command.getSagaId(), event);
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
            kafkaTemplate.send(commonEventsTopic, event.getSagaId(), event);
            log.info("Sent PAYMENT_METHOD_INVALID response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_CHECK_BALANCE command
     */
    public void handleCheckBalance(CommandMessage command) {
        Object amountObj = command.getPayloadValue("amount");
        String accountId = command.getPayloadValue("accountId");
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
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        Balance balance = balanceRepository.findBalanceByAccountId(accountId).orElse(null);
        if (balance == null) {
            handleCheckBalanceFailure(event, "BALANCE_NOT_FOUND", "Balance not found");
            return;
        }
        if (balance.getAvailable().compareTo(amount) < 0) {
            handleCheckBalanceFailure(event, "BALANCE_NOT_SUFFICIENT", "Balance is not sufficient");
        }

        event.setType("BALANCE_VALID");
        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
            log.info("Sent BALANCE_VALID response for saga: {}", event.getSagaId());
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle check balance failures
     */
    private void handleCheckBalanceFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("BALANCE_VALIDATION_ERROR");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);
        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
            log.info("Sent BALANCE_VALIDATION_ERROR response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION command
     */
    public void handleCreateDepositPendingTransaction(CommandMessage command) {
        log.info("Handling ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
//         Safe conversion from any numeric type to BigDecimal
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
            event.setType("DEPOSIT_TRANSACTION_CREATED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", savedTransaction.getId());
            event.setPayloadValue("status", savedTransaction.getStatus());
            event.setPayloadValue("amount", savedTransaction.getAmount());
            event.setPayloadValue("currency", savedTransaction.getCurrency());
            event.setPayloadValue("createdAt", savedTransaction.getCreatedAt().toString());

        } catch (Exception e) {
            log.error("Error creating transaction", e);
            handleCreateDepositPendingTransactionFailure(event, "TRANSACTION_CREATION_ERROR",
                    "Error creating transaction: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(depositEventsTopic, command.getSagaId(), event);
            log.info("Sent DEPOSIT_TRANSACTION_CREATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle transaction creation failures
     */
    private void handleCreateDepositPendingTransactionFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("DEPOSIT_TRANSACTION_CREATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(depositEventsTopic, event.getSagaId(), event);
            log.info("Sent DEPOSIT_TRANSACTION_CREATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION command
     */
    public void handleCreateWithdrawalPendingTransaction(CommandMessage command) {
        log.info("Handling ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION command for saga: {}", command.getSagaId());
        String accountId = command.getPayloadValue("accountId");
//         Safe conversion from any numeric type to BigDecimal
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
            transaction.setType("WITHDRAWAL"); // Using String type since we have a String field, not enum
            transaction.setStatus("PENDING"); // Using String status
            transaction.setPaymentMethodId(paymentMethodId);
            transaction.setAmount(amount);
            transaction.setCurrency(currency);
            transaction.setFee(BigDecimal.ZERO); // Set fee later if needed
            transaction.setDescription(description);
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());

            // Debug output
            log.debug("Transaction saved: {}", transaction);

            // Save the transaction
            Transaction transactionSaved = transactionRepository.save(transaction);

            // Set success response
            event.setType("WITHDRAWAL_TRANSACTION_CREATED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", transactionSaved.getId());
            event.setPayloadValue("status", transactionSaved.getStatus());
            event.setPayloadValue("amount", transactionSaved.getAmount());
            event.setPayloadValue("currency", transactionSaved.getCurrency());
            event.setPayloadValue("createdAt", transactionSaved.getCreatedAt().toString());

        } catch (Exception e) {
            log.error("Error creating withdrawal pending transaction", e);
            handleCreateWithdrawalPendingTransactionFailure(event, "TRANSACTION_CREATION_ERROR",
                    "Error creating transaction: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(withdrawalEventsTopic, command.getSagaId(), event);
            log.info("Sent WITHDRAWAL_TRANSACTION_CREATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }
    /**
     * Handle create withdrawal pending transaction failures
     */
    public void handleCreateWithdrawalPendingTransactionFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("WITHDRAWAL_TRANSACTION_CREATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);
        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
            log.info("Sent WITHDRAWAL_TRANSACTION_CREATION_FAILED response for saga: {} - {}",
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
            kafkaTemplate.send(commonEventsTopic, command.getSagaId(), event);
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
            kafkaTemplate.send(commonEventsTopic, event.getSagaId(), event);
            log.info("Sent TRANSACTION_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_DEPOSIT_UPDATE_BALANCE command
     */
    public void handleDepositUpdateBalance(CommandMessage command) {
        log.info("Handling ACCOUNT_DEPOSIT_UPDATE_BALANCE command for saga: {}", command.getSagaId());

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
                handleDepositUpdateBalanceFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find or create balance
            Balance balance = getOrCreateBalance(account);

            // Store original balance values
            BigDecimal originalAvailable = balance.getAvailable();
            BigDecimal originalTotal = balance.getTotal();

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
            event.setType("DEPOSIT_BALANCE_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("transactionId", transactionId);
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("updateType", "DEPOSIT");
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());
            // Saving original balance values for rollback if needed
            event.setPayloadValue("originalAvailableBalance", originalAvailable);
            event.setPayloadValue("originalTotalBalance", originalTotal);
            event.setPayloadValue("updateAmount", amount);

        } catch (Exception e) {
            log.error("Error updating balance", e);
            handleDepositUpdateBalanceFailure(event, "BALANCE_UPDATE_ERROR",
                    "Error updating balance: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(depositEventsTopic, command.getSagaId(), event);
            log.info("Sent DEPOSIT_BALANCE_UPDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle balance update failures
     */
    private void handleDepositUpdateBalanceFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("DEPOSIT_BALANCE_UPDATE_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(depositEventsTopic, event.getSagaId(), event);
            log.info("Sent DEPOSIT_BALANCE_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_WITHDRAWAL_UPDATE_BALANCE command
     */
    public void handleWithdrawalUpdateBalance(CommandMessage command) {
        log.info("Handling ACCOUNT_WITHDRAWAL_UPDATE_BALANCE command for saga: {}", command.getSagaId());

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

        if (shouldSimulateFailure()) {
            handleWithdrawalUpdateBalanceFailure(event, "WITHDRAWAL_UPDATE_BALANCE_FAILED", "Update balance failed!");
            return;
        }

        try {
            // Find account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleWithdrawalUpdateBalanceFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find or create balance
            Balance balance = getOrCreateBalance(account);

            // Store original balance values
            BigDecimal originalAvailable = balance.getAvailable();
            BigDecimal originalTotal = balance.getTotal();

            // Update balance
            BigDecimal newAvailable = balance.getAvailable().subtract(amount);
            BigDecimal newTotal = balance.getTotal().subtract(amount);

            balance.setAvailable(newAvailable);
            balance.setTotal(newTotal);
            balance.setUpdatedAt(Instant.now());

            log.debug("Updating balance: available={}, total={}", newAvailable, newTotal);

            // Save the updated balance
            balanceRepository.save(balance);

            // Set success response
            event.setType("WITHDRAWAL_BALANCE_UPDATED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("transactionId", transactionId);
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("updateType", "WITHDRAWAL");
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());
            // Saving original balance values for rollback if needed
            event.setPayloadValue("originalAvailableBalance", originalAvailable);
            event.setPayloadValue("originalTotalBalance", originalTotal);
            event.setPayloadValue("updateAmount", amount);

        } catch (Exception e) {
            log.error("Error updating balance", e);
            handleWithdrawalUpdateBalanceFailure(event, "BALANCE_UPDATE_ERROR",
                    "Error updating balance: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(withdrawalEventsTopic, command.getSagaId(), event);
            log.info("Sent WITHDRAWAL_BALANCE_UPDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle update balance failures for withdrawal case
     */
    public void handleWithdrawalUpdateBalanceFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("WITHDRAWAL_BALANCE_UPDATE_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
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
            event.setPayloadValue("errorCode", errorCode);
            event.setPayloadValue("updatedAt", updatedTransaction.getUpdatedAt().toString());

        } catch (Exception e) {
            log.error("Error marking transaction as failed", e);
            handleTransactionFailureError(event, "TRANSACTION_UPDATE_ERROR",
                    "Error marking transaction as failed: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(commonEventsTopic, command.getSagaId(), event);
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
            kafkaTemplate.send(commonEventsTopic, event.getSagaId(), event);
            log.info("Sent TRANSACTION_MARK_FAILED_ERROR response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE command
     */
    public void handleDepositReverseBalanceUpdate(CommandMessage command) {
        log.info("Handling ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE command for saga: {}", command.getSagaId());

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
                handleDepositBalanceReverseFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            // Find balance
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                handleDepositBalanceReverseFailure(event, "BALANCE_NOT_FOUND",
                        "Balance not found for account: " + accountId);
                return;
            }

            // Get expected values from the original operation
            BigDecimal originalAmount = command.getPayloadValue("amount");
            BigDecimal expectedAvailable = null;

            // Get the original event data if possible from the saga
            if (command.getMetadataValue("originalEventAvailable") != null) {
                expectedAvailable = new BigDecimal(command.getMetadataValue("originalEventAvailable"));
            }

            // Check if compensation is actually needed
            boolean needsCompensation = true;

            // If we have the data to verify, check if the update was actually completed
            if (expectedAvailable != null) {
                // If current balance doesn't match what we'd expect after the update,
                // the original operation likely didn't complete
                if (Math.abs(balance.getAvailable().subtract(expectedAvailable).doubleValue()) > 0.001) {
                    log.info("Skipping compensation as balance update likely didn't complete (current: {}, expected: {})",
                            balance.getAvailable(), expectedAvailable);
                    needsCompensation = false;
                }
            }

            Transaction reversalTransaction = new Transaction();

            // Initialize balance variables regardless of compensation path
            BigDecimal newAvailable = balance.getAvailable();
            BigDecimal newTotal = balance.getTotal();

            if (needsCompensation) {
                // Reverse the balance update (subtract the amount since it was a deposit)
                newAvailable = balance.getAvailable().subtract(amount);
                newTotal = balance.getTotal().subtract(amount);

                balance.setAvailable(newAvailable);
                balance.setTotal(newTotal);
                balance.setUpdatedAt(Instant.now());

                log.debug("Reversing balance update: available={}, total={}", newAvailable, newTotal);

                // Save the updated balance
                balanceRepository.save(balance);

                // Create a reversal transaction record

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
            } else {
                // If no compensation is needed, just log it
                reversalTransaction.setId(UUID.randomUUID().toString());
                reversalTransaction.setAccountId(accountId);
                reversalTransaction.setType("DEPOSIT_REVERSAL_SKIPPED");
                reversalTransaction.setStatus("COMPLETED");
                reversalTransaction.setAmount(BigDecimal.ZERO); // No actual change
                reversalTransaction.setCurrency(balance.getCurrency());
                reversalTransaction.setFee(BigDecimal.ZERO);
                reversalTransaction.setDescription("Reversal skipped as original update did not complete");
                reversalTransaction.setCreatedAt(Instant.now());
                reversalTransaction.setUpdatedAt(Instant.now());
                reversalTransaction.setCompletedAt(Instant.now());
                reversalTransaction.setExternalReferenceId("REV-" + transactionId);

                transactionRepository.save(reversalTransaction);
            }


            // Set success response
            event.setType("DEPOSIT_BALANCE_REVERSAL_COMPLETED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("originalTransactionId", transactionId);
            event.setPayloadValue("reversalTransactionId", reversalTransaction.getId());
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());
            event.setPayloadValue("reversedAmount", amount);

        } catch (Exception e) {
            log.error("Error reversing balance update", e);
            handleDepositBalanceReverseFailure(event, "BALANCE_REVERSAL_ERROR",
                    "Error reversing balance update: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(depositEventsTopic, command.getSagaId(), event);
            log.info("Sent DEPOSIT_BALANCE_REVERSAL_COMPLETED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method for balance reversal failures
     */
    private void handleDepositBalanceReverseFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("DEPOSIT_BALANCE_REVERSAL_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(depositEventsTopic, event.getSagaId(), event);
            log.info("Sent DEPOSIT_BALANCE_REVERSAL_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE command
     */
    public void handleWithdrawalReverseBalanceUpdate(CommandMessage command) {
        log.info("Handling ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE command for saga: {}", command.getSagaId());

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
                handleDepositBalanceReverseFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            // Find balance
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                handleDepositBalanceReverseFailure(event, "BALANCE_NOT_FOUND",
                        "Balance not found for account: " + accountId);
                return;
            }

            Transaction reversalTransaction = new Transaction();
            BigDecimal newAvailable = balance.getAvailable();
            BigDecimal newTotal = balance.getTotal();

            if (!(amount.compareTo(BigDecimal.ZERO) == 0)) {
                newAvailable = balance.getAvailable().add(amount);
                newTotal = balance.getTotal().add(amount);

                balance.setUpdatedAt(Instant.now());
                balance.setAvailable(newAvailable);
                balance.setTotal(newTotal);

                log.debug("Reversing balance update: available={}, total={}", newAvailable, newTotal);

                // Save the updated balance
                balanceRepository.save(balance);

                // Create a reversal transaction record

                reversalTransaction.setId(UUID.randomUUID().toString());
                reversalTransaction.setAccountId(accountId);
                reversalTransaction.setType("WITHDRAWAL_REVERSAL");
                reversalTransaction.setStatus("COMPLETED");
                reversalTransaction.setAmount(amount);
                reversalTransaction.setCurrency(balance.getCurrency());
                reversalTransaction.setFee(BigDecimal.ZERO);
                reversalTransaction.setDescription("Reversal of withdrawal due to: " + reason);
                reversalTransaction.setCreatedAt(Instant.now());
                reversalTransaction.setUpdatedAt(Instant.now());
                reversalTransaction.setCompletedAt(Instant.now());
                reversalTransaction.setExternalReferenceId("REV-" + transactionId);

                transactionRepository.save(reversalTransaction);
            } else {
                // If no compensation is needed, just log it
                reversalTransaction.setId(UUID.randomUUID().toString());
                reversalTransaction.setAccountId(accountId);
                reversalTransaction.setType("WITHDRAWAL_REVERSAL_SKIPPED");
                reversalTransaction.setStatus("COMPLETED");
                reversalTransaction.setAmount(BigDecimal.ZERO);
                reversalTransaction.setCurrency(balance.getCurrency());
                reversalTransaction.setFee(BigDecimal.ZERO);
                reversalTransaction.setDescription("Reversal skipped as original update did not complete");
                reversalTransaction.setCreatedAt(Instant.now());
                reversalTransaction.setUpdatedAt(Instant.now());
                reversalTransaction.setCompletedAt(Instant.now());
                reversalTransaction.setExternalReferenceId("REV-" + transactionId);

                transactionRepository.save(reversalTransaction);
            }


            // Set success response
            event.setType("WITHDRAWAL_BALANCE_REVERSAL_COMPLETED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("originalTransactionId", transactionId);
            event.setPayloadValue("reversalTransactionId", reversalTransaction.getId());
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newTotalBalance", newTotal);
            event.setPayloadValue("reversedAmount", amount);
            event.setPayloadValue("updatedAt", balance.getUpdatedAt().toString());

//            if (shouldSimulateFailure()) {
//                handleWithdrawalReverseBalanceUpdateFailure(event, "WITHDRAWAL_BALANCE_REVERSAL_ERROR", "Error reversing balance update!");
//            }
        } catch(Exception e){
            log.error("Error reversing balance update", e);
            handleWithdrawalReverseBalanceUpdateFailure(event, "WITHDRAWAL_BALANCE_REVERSAL_ERROR",
                    "Error reversing balance update: " + e.getMessage());
            return;
        }
        // Send the response event
        try {
            kafkaTemplate.send(withdrawalEventsTopic, command.getSagaId(), event);
            log.info("Sent WITHDRAWAL_BALANCE_REVERSAL_COMPLETED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle reversal update balance failures for withdrawal
     */
    public void handleWithdrawalReverseBalanceUpdateFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("WITHDRAWAL_BALANCE_REVERSAL_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
            log.info("Sent WITHDRAWAL_BALANCE_REVERSAL_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_VERIFY_STATUS command
     */
    public void handleVerifyAccountStatus(CommandMessage command) {
        log.info("Handling ACCOUNT_VERIFY_STATUS command for saga: {}", command.getSagaId());

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
            // Validate account exists and belongs to the user
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);

            if (accountOpt.isEmpty()) {
                handleAccountVerificationFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Verify ownership
            if (!account.getUserId().equals(userId)) {
                handleAccountVerificationFailure(event, "ACCOUNT_NOT_AUTHORIZED",
                        "Account does not belong to user");
                return;
            }

            // Verify account status
            if (!account.getStatus().equals("ACTIVE")) {
                handleAccountVerificationFailure(event, "ACCOUNT_NOT_ACTIVE",
                        "Account is not active: " + account.getStatus());
                return;
            }

            // Verify account has sufficient balance or credit
            // This is optional and depends on your requirements
            // You might want to check if the account has any funds at all

            // All validations passed
            event.setType("ACCOUNT_STATUS_VERIFIED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("accountStatus", account.getStatus());
            event.setPayloadValue("userId", userId);

        } catch (Exception e) {
            log.error("Error verifying account status", e);
            handleAccountVerificationFailure(event, "VERIFICATION_ERROR",
                    "Error verifying account status: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
            log.info("Sent ACCOUNT_STATUS_VERIFIED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ACCOUNT_VERIFY_STATUS command for sell orders
     */
    public void handleVerifyAccountStatusSell(CommandMessage command) {
        log.info("Handling ACCOUNT_VERIFY_STATUS command for sell saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Validate account exists and belongs to the user
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);

            if (accountOpt.isEmpty()) {
                handleAccountVerificationFailureSell(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Verify ownership
            if (!account.getUserId().equals(userId)) {
                handleAccountVerificationFailureSell(event, "ACCOUNT_NOT_AUTHORIZED",
                        "Account does not belong to user");
                return;
            }

            // Verify account status
            if (!account.getStatus().equals("ACTIVE")) {
                handleAccountVerificationFailureSell(event, "ACCOUNT_NOT_ACTIVE",
                        "Account is not active: " + account.getStatus());
                return;
            }

            // All validations passed
            event.setType("ACCOUNT_STATUS_VERIFIED");
            event.setSuccess(true);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("accountStatus", account.getStatus());
            event.setPayloadValue("userId", userId);

        } catch (Exception e) {
            log.error("Error verifying account status for sell order", e);
            handleAccountVerificationFailureSell(event, "VERIFICATION_ERROR",
                    "Error verifying account status: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent ACCOUNT_STATUS_VERIFIED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle account verification failures for sell orders
     */
    private void handleAccountVerificationFailureSell(EventMessage event, String errorCode, String errorMessage) {
        event.setType("ACCOUNT_STATUS_INVALID");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderSellEventsTopic, event.getSagaId(), event);
            log.info("Sent ACCOUNT_STATUS_INVALID response for sell saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method to handle account verification failures
     */
    private void handleAccountVerificationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("ACCOUNT_STATUS_INVALID");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.order-buy", event.getSagaId(), event);
            log.info("Sent ACCOUNT_STATUS_INVALID response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_RESERVE_FUNDS command
     */
    public void handleReserveFunds(CommandMessage command) {
        log.info("Handling ACCOUNT_RESERVE_FUNDS command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String orderId = command.getPayloadValue("orderId");
        String stockSymbol = command.getPayloadValue("stockSymbol");

        // Handle different possible formats of amount
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);

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
                handleReservationFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find or create balance
            Balance balance = getOrCreateBalance(account);

            // Check if sufficient funds are available
            if (balance.getAvailable().compareTo(amount) < 0) {
                handleReservationFailure(event, "INSUFFICIENT_FUNDS",
                        "Insufficient funds available. Required: " + amount + ", Available: " + balance.getAvailable());
                return;
            }

            // Create reservation record
            ReservationRecord reservation = new ReservationRecord();
            reservation.setId(UUID.randomUUID().toString());
            reservation.setAccountId(accountId);
            reservation.setAmount(amount);
            reservation.setCurrency(balance.getCurrency());
            reservation.setPurpose("ORDER_BUY");
            reservation.setReferenceId(orderId);
            reservation.setCreatedAt(Instant.now());
            // Set expiration time (e.g., 1 day from now)
            reservation.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
            reservation.setStatus(ReservationRecord.ReservationStatus.ACTIVE.toString());

            // Update balance (move from available to reserved)
            BigDecimal newAvailable = balance.getAvailable().subtract(amount);
            BigDecimal newReserved = balance.getReserved().add(amount);

            balance.setAvailable(newAvailable);
            balance.setReserved(newReserved);
            balance.setUpdatedAt(Instant.now());

            // Save both the reservation and updated balance
            ReservationRecord savedReservation = reservationRecordRepository.save(reservation);
            balanceRepository.save(balance);

            log.info("Successfully reserved funds: {} for order: {}", amount, orderId);

            // Set success response
            event.setType("FUNDS_RESERVED");
            event.setSuccess(true);
            event.setPayloadValue("reservationId", savedReservation.getId());
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("reservedAt", savedReservation.getCreatedAt().toString());
            event.setPayloadValue("expiresAt", savedReservation.getExpiresAt().toString());
            event.setPayloadValue("newAvailableBalance", newAvailable);
            event.setPayloadValue("newReservedBalance", newReserved);
            event.setPayloadValue("newTotalBalance", balance.getTotal());

        } catch (Exception e) {
            log.error("Error reserving funds", e);
            handleReservationFailure(event, "RESERVATION_ERROR",
                    "Error reserving funds: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
            log.info("Sent FUNDS_RESERVED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle reservation failures
     */
    private void handleReservationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("FUNDS_RESERVATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.order-buy", event.getSagaId(), event);
            log.info("Sent FUNDS_RESERVATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
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
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
    }

    /**
     * Handle ACCOUNT_RELEASE_FUNDS command (compensation)
     */
    public void handleReleaseFunds(CommandMessage command) {
        log.info("Handling ACCOUNT_RELEASE_FUNDS command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String reservationId = command.getPayloadValue("reservationId");
        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find the reservation
            Optional<ReservationRecord> reservationOpt = reservationRecordRepository.findById(reservationId);

            // If reservation doesn't exist, it could be that it was already released or never created
            if (reservationOpt.isEmpty()) {
                log.warn("Reservation not found for ID: {}. It may have already been released.", reservationId);

                // Still send a success event to continue the compensation flow
                event.setType("FUNDS_RELEASED");
                event.setSuccess(true);
                event.setPayloadValue("reservationId", reservationId);
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("releasedAt", Instant.now().toString());
                event.setPayloadValue("status", "ALREADY_RELEASED");

                kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
                return;
            }

            ReservationRecord reservation = reservationOpt.get();

            // Only process if the reservation is still active
            if (reservation.getStatus().equals(ReservationRecord.ReservationStatus.ACTIVE.toString())) {
                // Find account balance
                Balance balance = balanceRepository.findByAccountId(reservation.getAccountId());

                if (balance != null) {
                    // Update balance (move from reserved back to available)
                    BigDecimal reservedAmount = reservation.getAmount();
                    BigDecimal newAvailable = balance.getAvailable().add(reservedAmount);
                    BigDecimal newReserved = balance.getReserved().subtract(reservedAmount);

                    balance.setAvailable(newAvailable);
                    balance.setReserved(newReserved);
                    balance.setUpdatedAt(Instant.now());

                    // Save the updated balance
                    balanceRepository.save(balance);

                    log.info("Released reserved funds: {} for order: {}", reservedAmount, orderId);
                }

                // Update reservation status
                reservation.setStatus(ReservationRecord.ReservationStatus.FULLY_RELEASED.toString());
                reservationRecordRepository.save(reservation);
            } else {
                log.info("Reservation {} is already in state: {}. No action needed.",
                        reservationId, reservation.getStatus());
            }

            // Set success response
            event.setType("FUNDS_RELEASED");
            event.setSuccess(true);
            event.setPayloadValue("reservationId", reservationId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("releasedAt", Instant.now().toString());
            event.setPayloadValue("previousStatus", reservation.getStatus());
            event.setPayloadValue("newStatus", ReservationRecord.ReservationStatus.FULLY_RELEASED.toString());

        } catch (Exception e) {
            log.error("Error releasing funds", e);
            event.setType("FUNDS_RELEASE_FAILED");
            event.setSuccess(false);
            event.setErrorCode("RELEASE_ERROR");
            event.setErrorMessage("Error releasing funds: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
            log.info("Sent funds release response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ACCOUNT_SETTLE_TRANSACTION command
     * This finalizes a stock purchase by converting reserved funds into an actual deduction
     */
    public void handleSettleTransaction(CommandMessage command) {
        log.info("Handling ACCOUNT_SETTLE_TRANSACTION command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String reservationId = command.getPayloadValue("reservationId");
        String orderId = command.getPayloadValue("orderId");

        // Get the final amount to settle (could be different from initial reserved amount)
        Object finalAmountObj = command.getPayloadValue("finalAmount");
        BigDecimal finalAmount = convertToBigDecimal(finalAmountObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

//        handleSettlementFailure(event, "SETTLEMENT_ERROR",
//                "Error settling transaction: ");

        try {
            // Find the account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleSettlementFailure(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }
            TradingAccount account = accountOpt.get();

            // Find the reservation
            Optional<ReservationRecord> reservationOpt = reservationRecordRepository.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                handleSettlementFailure(event, "RESERVATION_NOT_FOUND",
                        "Fund reservation not found: " + reservationId);
                return;
            }
            ReservationRecord reservation = reservationOpt.get();

            // Validate reservation status
            if (!reservation.getStatus().equals(ReservationRecord.ReservationStatus.ACTIVE.toString())) {
                handleSettlementFailure(event, "INVALID_RESERVATION_STATUS",
                        "Reservation is not active: " + reservation.getStatus());
                return;
            }

            // Get balance
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                handleSettlementFailure(event, "BALANCE_NOT_FOUND",
                        "Balance record not found for account: " + accountId);
                return;
            }

            // Store original values (for potential compensation)
            BigDecimal originalReserved = balance.getReserved();
            BigDecimal originalTotal = balance.getTotal();

            // Calculate the difference between reserved and final amount
            // This handles cases where execution price was different than estimated
            BigDecimal reservedAmount = reservation.getAmount();
            BigDecimal refundAmount = BigDecimal.ZERO;

            if (reservedAmount.compareTo(finalAmount) > 0) {
                // We reserved more than needed, calculate refund
                refundAmount = reservedAmount.subtract(finalAmount);
            }

            // Update the balance:
            // 1. Remove from reserved
            // 2. Decrease total by finalAmount
            // 3. Add refund to available if applicable
            balance.setReserved(balance.getReserved().subtract(reservedAmount));
            balance.setTotal(balance.getTotal().subtract(finalAmount));

            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                balance.setAvailable(balance.getAvailable().add(refundAmount));
            }

            balance.setUpdatedAt(Instant.now());

            // Mark reservation as settled
            reservation.setStatus(ReservationRecord.ReservationStatus.SETTLED.toString());
            reservation.setUpdatedAt(Instant.now());

            // Create a transaction record
            Transaction transaction = new Transaction();
            transaction.setId(UUID.randomUUID().toString());
            transaction.setAccountId(accountId);
            transaction.setType("ORDER_PAYMENT");
            transaction.setStatus("COMPLETED");
            transaction.setAmount(finalAmount.negate()); // Negative amount for payment
            transaction.setCurrency(balance.getCurrency());
            transaction.setFee(BigDecimal.ZERO); // Set fee as needed
            transaction.setDescription("Payment for order " + orderId);
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            transaction.setCompletedAt(Instant.now());
            transaction.setExternalReferenceId(orderId);

            // Save all updates
            balanceRepository.save(balance);
            reservationRecordRepository.save(reservation);
            transactionRepository.save(transaction);

            // Set success response
            event.setType("TRANSACTION_SETTLED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", transaction.getId());
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("reservationId", reservationId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("amount", finalAmount);
            event.setPayloadValue("newTotalBalance", balance.getTotal());
            event.setPayloadValue("refundAmount", refundAmount);

            // Save original values for potential compensation
            event.setPayloadValue("originalReserved", originalReserved);
            event.setPayloadValue("originalTotal", originalTotal);
            event.setPayloadValue("settlementTime", transaction.getCompletedAt().toString());

            log.info("Transaction settled successfully for account: {}, order: {}", accountId, orderId);

        } catch (Exception e) {
            log.error("Error settling transaction", e);
            handleSettlementFailure(event, "SETTLEMENT_ERROR",
                    "Error settling transaction: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
            log.info("Sent TRANSACTION_SETTLED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle settlement failures
     */
    private void handleSettlementFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("TRANSACTION_SETTLEMENT_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.order-buy", event.getSagaId(), event);
            log.info("Sent TRANSACTION_SETTLEMENT_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_REVERSE_SETTLEMENT command (compensation)
     * This undoes a previous settlement when part of the saga fails
     */
    public void handleReverseSettlement(CommandMessage command) {
        log.info("Handling ACCOUNT_REVERSE_SETTLEMENT command for saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String reservationId = command.getPayloadValue("reservationId");
        String orderId = command.getPayloadValue("orderId");

        // Get the amount to reverse
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find the account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                // Even in compensation, we try to continue the saga
                log.warn("Account not found during settlement reversal: {}", accountId);
                event.setType("SETTLEMENT_REVERSED");
                event.setSuccess(true); // Return success to continue compensation
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("status", "ACCOUNT_NOT_FOUND");

                kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find relevant balance record
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                log.warn("Balance not found during settlement reversal: {}", accountId);
                event.setType("SETTLEMENT_REVERSED");
                event.setSuccess(true); // Return success to continue compensation
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("status", "BALANCE_NOT_FOUND");

                kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
                return;
            }

            // Create transaction record for reversal
            Transaction reversal = new Transaction();
            reversal.setId(UUID.randomUUID().toString());
            reversal.setAccountId(accountId);
            reversal.setType("ORDER_PAYMENT_REVERSAL");
            reversal.setStatus("COMPLETED");
            reversal.setAmount(amount); // Positive amount (returning funds)
            reversal.setCurrency(balance.getCurrency());
            reversal.setFee(BigDecimal.ZERO);
            reversal.setDescription("Reversal of payment for order " + orderId);
            reversal.setCreatedAt(Instant.now());
            reversal.setUpdatedAt(Instant.now());
            reversal.setCompletedAt(Instant.now());
            reversal.setExternalReferenceId("REV-" + orderId);

            // Update balance
            balance.setAvailable(balance.getAvailable().add(amount));
            balance.setTotal(balance.getTotal().add(amount));
            balance.setUpdatedAt(Instant.now());

            // Save changes
            balanceRepository.save(balance);
            transactionRepository.save(reversal);

            // If the reservation record still exists, update it
            Optional<ReservationRecord> reservationOpt = reservationRecordRepository.findById(reservationId);
            if (reservationOpt.isPresent()) {
                ReservationRecord reservation = reservationOpt.get();
                reservation.setStatus(ReservationRecord.ReservationStatus.REVERSED.toString());
                reservation.setUpdatedAt(Instant.now());
                reservationRecordRepository.save(reservation);
            }

            // Set success response
            event.setType("SETTLEMENT_REVERSED");
            event.setSuccess(true);
            event.setPayloadValue("reversalTransactionId", reversal.getId());
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("newAvailableBalance", balance.getAvailable());
            event.setPayloadValue("newTotalBalance", balance.getTotal());
            event.setPayloadValue("reversedAt", reversal.getCompletedAt().toString());

            log.info("Settlement successfully reversed for account: {}, order: {}", accountId, orderId);

        } catch (Exception e) {
            log.error("Error reversing settlement", e);
            // Even in case of error, we want the compensation to continue
            event.setType("SETTLEMENT_REVERSED");
            event.setSuccess(true); // Return success to continue compensation
            event.setPayloadValue("error", true);
            event.setPayloadValue("errorMessage", e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.order-buy", command.getSagaId(), event);
            log.info("Sent SETTLEMENT_REVERSED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ACCOUNT_SETTLE_TRANSACTION command for sell orders
     * This credits funds to the account after a successful stock sale
     */
    public void handleSettleTransactionSell(CommandMessage command) {
        log.info("Handling ACCOUNT_SETTLE_TRANSACTION command for sell saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String orderId = command.getPayloadValue("orderId");
        String transactionType = command.getPayloadValue("transactionType");

        // Get the amount to credit - expected to be positive for sell orders
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find the account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                handleSettlementFailureSell(event, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountId);
                return;
            }
            TradingAccount account = accountOpt.get();

            // Get balance
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                // If no balance exists, create one
                balance = new Balance();
                balance.setId(UUID.randomUUID().toString());
                balance.setAccountId(accountId);
                balance.setCurrency("USD"); // Default currency
                balance.setAvailable(BigDecimal.ZERO);
                balance.setReserved(BigDecimal.ZERO);
                balance.setTotal(BigDecimal.ZERO);
            }

            // Store original values (for potential compensation)
            BigDecimal originalAvailable = balance.getAvailable();
            BigDecimal originalTotal = balance.getTotal();

            // Update the balance (credit the funds)
            balance.setAvailable(balance.getAvailable().add(amount));
            balance.setTotal(balance.getTotal().add(amount));
            balance.setUpdatedAt(Instant.now());

            // Create a transaction record
            Transaction transaction = new Transaction();
            transaction.setId(UUID.randomUUID().toString());
            transaction.setAccountId(accountId);
            transaction.setType("ORDER_SELL_PAYMENT");
            transaction.setStatus("COMPLETED");
            transaction.setAmount(amount); // Positive amount for credit
            transaction.setCurrency(balance.getCurrency());
            transaction.setFee(BigDecimal.ZERO); // Set fee as needed
            transaction.setDescription("Payment for sell order " + orderId);
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            transaction.setCompletedAt(Instant.now());
            transaction.setExternalReferenceId(orderId);

            // Save all updates
            balanceRepository.save(balance);
            transactionRepository.save(transaction);

            // Set success response
            event.setType("TRANSACTION_SETTLED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", transaction.getId());
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("newAvailableBalance", balance.getAvailable());
            event.setPayloadValue("newTotalBalance", balance.getTotal());

            // Save original values for potential compensation
            event.setPayloadValue("originalAvailableBalance", originalAvailable);
            event.setPayloadValue("originalTotalBalance", originalTotal);
            event.setPayloadValue("settlementTime", transaction.getCompletedAt().toString());

            log.info("Sell order funds credited successfully for account: {}, order: {}, amount: {}",
                    accountId, orderId, amount);

        } catch (Exception e) {
            log.error("Error settling sell transaction", e);
            handleSettlementFailureSell(event, "SETTLEMENT_ERROR",
                    "Error settling sell transaction: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent TRANSACTION_SETTLED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle settlement failures for sell orders
     */
    private void handleSettlementFailureSell(EventMessage event, String errorCode, String errorMessage) {
        event.setType("TRANSACTION_SETTLEMENT_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderSellEventsTopic, event.getSagaId(), event);
            log.info("Sent TRANSACTION_SETTLEMENT_FAILED response for sell saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ACCOUNT_REVERSE_SETTLEMENT command for sell orders (compensation)
     * This undoes a previous settlement when part of the saga fails
     */
    public void handleReverseSettlementSell(CommandMessage command) {
        log.info("Handling ACCOUNT_REVERSE_SETTLEMENT command for sell saga: {}", command.getSagaId());

        String accountId = command.getPayloadValue("accountId");
        String orderId = command.getPayloadValue("orderId");

        // Get the amount to reverse (should be the original credited amount)
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Find the account
            Optional<TradingAccount> accountOpt = tradingAccountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                // Even in compensation, we try to continue the saga
                log.warn("Account not found during settlement reversal: {}", accountId);
                event.setType("SETTLEMENT_REVERSED");
                event.setSuccess(true); // Return success to continue compensation
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("status", "ACCOUNT_NOT_FOUND");

                kafkaTemplate.send(orderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            TradingAccount account = accountOpt.get();

            // Find relevant balance record
            Balance balance = balanceRepository.findByAccountId(accountId);
            if (balance == null) {
                log.warn("Balance not found during settlement reversal: {}", accountId);
                event.setType("SETTLEMENT_REVERSED");
                event.setSuccess(true); // Return success to continue compensation
                event.setPayloadValue("accountId", accountId);
                event.setPayloadValue("status", "BALANCE_NOT_FOUND");

                kafkaTemplate.send(orderSellEventsTopic, command.getSagaId(), event);
                return;
            }

            // Create transaction record for reversal
            Transaction reversal = new Transaction();
            reversal.setId(UUID.randomUUID().toString());
            reversal.setAccountId(accountId);
            reversal.setType("ORDER_SELL_PAYMENT_REVERSAL");
            reversal.setStatus("COMPLETED");
            reversal.setAmount(amount.negate()); // Negative amount (removing funds)
            reversal.setCurrency(balance.getCurrency());
            reversal.setFee(BigDecimal.ZERO);
            reversal.setDescription("Reversal of payment for sell order " + orderId);
            reversal.setCreatedAt(Instant.now());
            reversal.setUpdatedAt(Instant.now());
            reversal.setCompletedAt(Instant.now());
            reversal.setExternalReferenceId("REV-" + orderId);

            // Update balance (reversal of credit)
            BigDecimal newAvailable = balance.getAvailable().subtract(amount);

            // Safeguard against negative balance - unlikely but good practice
            if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Settlement reversal would cause negative balance for account: {}. Limiting to zero.", accountId);
                newAvailable = BigDecimal.ZERO;
            }

            BigDecimal newTotal = balance.getTotal().subtract(amount);
            if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
                newTotal = BigDecimal.ZERO;
            }

            balance.setAvailable(newAvailable);
            balance.setTotal(newTotal);
            balance.setUpdatedAt(Instant.now());

            // Save changes
            balanceRepository.save(balance);
            transactionRepository.save(reversal);

            // Set success response
            event.setType("SETTLEMENT_REVERSED");
            event.setSuccess(true);
            event.setPayloadValue("reversalTransactionId", reversal.getId());
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("newAvailableBalance", balance.getAvailable());
            event.setPayloadValue("newTotalBalance", balance.getTotal());
            event.setPayloadValue("reversedAt", reversal.getCompletedAt().toString());

            log.info("Settlement successfully reversed for sell order - account: {}, order: {}", accountId, orderId);

        } catch (Exception e) {
            log.error("Error reversing sell settlement", e);
            // Even in case of error, we want the compensation to continue
            event.setType("SETTLEMENT_REVERSED");
            event.setSuccess(true); // Return success to continue compensation
            event.setPayloadValue("error", true);
            event.setPayloadValue("errorMessage", e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderSellEventsTopic, command.getSagaId(), event);
            log.info("Sent SETTLEMENT_REVERSED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Should we simulate a failure? (10% chance)
     */
    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < 10;
    }
}
