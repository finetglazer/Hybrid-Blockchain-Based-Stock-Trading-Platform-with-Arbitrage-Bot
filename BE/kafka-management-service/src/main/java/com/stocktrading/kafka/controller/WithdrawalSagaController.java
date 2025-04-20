package com.stocktrading.kafka.controller;


import com.stocktrading.kafka.dto.*;
import com.stocktrading.kafka.model.WithdrawalSagaState;
import com.stocktrading.kafka.service.WithdrawalSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("sagas/api/v1/withdrawal")
@RequiredArgsConstructor
public class WithdrawalSagaController {

    private final WithdrawalSagaService withdrawalSagaService;

    /**
     * Start a new withdrawal saga
     */
    @PostMapping("/start")
    public ResponseEntity<WithdrawalSagaDto> startWithdrawalSaga(@Valid @RequestBody WithdrawalSagaRequest request) {
        log.info("Received request to start withdrawal saga for account: {}", request.getAccountId());

        WithdrawalSagaState saga = withdrawalSagaService.startWithdrawalSaga(
                request.getUserId(),
                request.getAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethodId(),
                request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToWithdrawalDto(saga));
    }

    /**
     * Map a saga state entity to a Withdrawal DTO
     */
    private WithdrawalSagaDto mapToWithdrawalDto(WithdrawalSagaState saga) {
        WithdrawalSagaDto dto = new WithdrawalSagaDto();
        dto.setSagaId(saga.getSagaId());
        dto.setUserId(saga.getUserId());
        dto.setAccountId(saga.getAccountId());
        dto.setAmount(saga.getAmount());
        dto.setCurrency(saga.getCurrency());
        dto.setTransactionId(saga.getTransactionId());
        dto.setPaymentMethodId(saga.getPaymentMethodId());

        if (saga.getCurrentStep() != null) {
            dto.setCurrentStepNumber(saga.getCurrentStep().getStepNumber());
            dto.setCurrentStep(saga.getCurrentStep().name());
        }

        dto.setStatus(saga.getStatus().name());
        dto.setCompletedSteps(saga.getCompletedSteps());
        dto.setStartTime(saga.getStartTime());
        dto.setEndTime(saga.getEndTime());
        dto.setLastUpdatedTime(saga.getLastUpdatedTime());

        // Filter events to a reasonable number (last 10)
        if (saga.getSagaEvents() != null && !saga.getSagaEvents().isEmpty()) {
            int eventsSize = saga.getSagaEvents().size();
            int startIndex = Math.max(0, eventsSize - 10);
            dto.setRecentEvents(saga.getSagaEvents().subList(startIndex, eventsSize));
        }

        dto.setFailureReason(saga.getFailureReason());
        dto.setRetryCount(saga.getRetryCount());
        dto.setMaxRetries(saga.getMaxRetries());

        return dto;
    }
}
