package com.stocktrading.kafka.model.enums;

import com.project.kafkamessagemodels.model.enums.CommandType;
import lombok.Getter;

@Getter
public enum WithdrawalSagaStep {
    // Normal flow steps
    START (0, "Saga created", CommandType.START),
    USER_VERIFY_IDENTITY(1, "Verify user identity", CommandType.USER_VERIFY_IDENTITY),
    ACCOUNT_VALIDATE(2, "Validate account", CommandType.ACCOUNT_VALIDATE),
    ACCOUNT_CHECK_BALANCE(3, "Check balance", CommandType.ACCOUNT_CHECK_BALANCE),
    PAYMENT_METHOD_VALIDATE(4, "Validate payment method", CommandType.PAYMENT_METHOD_VALIDATE),
    ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION(5, "Create pending withdrawal transaction", CommandType.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION),
    PAYMENT_PROCESS_WITHDRAWAL(6, "Process withdrawal", CommandType.PAYMENT_PROCESS_WITHDRAWAL),
    ACCOUNT_UPDATE_TRANSACTION_STATUS(7, "Update transaction status", CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS),
    ACCOUNT_WITHDRAWAL_UPDATE_BALANCE(8, "Update balance", CommandType.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE),
    COMPLETE_SAGA(9, "Complete saga", null),

    // Compensation steps
    START_COMPENSATION(100, "Start compensation", CommandType.START_COMPENSATION),
    ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE(101, "Reverse balance update", CommandType.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE),
    PAYMENT_REVERSE_WITHDRAWAL(102, "Reverse payment", CommandType.PAYMENT_REVERSE_WITHDRAWAL),
    ACCOUNT_MARK_TRANSACTION_FAILED(103, "Mark transaction failed", CommandType.ACCOUNT_MARK_TRANSACTION_FAILED),
    COMPLETE_COMPENSATION(104, "Complete compensation", null);

    private final int stepNumber;
    private final String description;
    private final CommandType commandType;

    WithdrawalSagaStep(int stepNumber, String description, CommandType commandType) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.commandType = commandType;
    }
}
