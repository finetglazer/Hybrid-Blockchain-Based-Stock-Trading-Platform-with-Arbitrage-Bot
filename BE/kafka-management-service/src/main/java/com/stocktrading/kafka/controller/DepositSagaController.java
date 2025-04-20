package com.stocktrading.kafka.controller;


import com.stocktrading.kafka.dto.*;
import com.stocktrading.kafka.model.DepositSagaState;
import com.stocktrading.kafka.service.DepositSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for saga management
 */
@Slf4j
@RestController
@RequestMapping("sagas/api/v1/deposit")
@RequiredArgsConstructor
public class DepositSagaController {
    
    private final DepositSagaService depositSagaService;

    /**
     * Start a new deposit saga
     */
    @PostMapping("/start")
    public ResponseEntity<DepositSagaDto> startDepositSaga(@Valid @RequestBody DepositSagaRequest request) {
        log.info("Received request to start deposit saga for account: {}", request.getAccountId());
        
        DepositSagaState saga = depositSagaService.startSaga(
            request.getUserId(),
            request.getAccountId(),
            request.getAmount(),
            request.getCurrency(),
            request.getPaymentMethodId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDepositDto(saga));
    }

    /**
     * Get a saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<DepositSagaDto> getSaga(@PathVariable String sagaId) {
        log.info("Received request to get saga: {}", sagaId);

        Optional<DepositSagaState> optionalSaga = depositSagaService.findById(sagaId);
        
        return optionalSaga
            .map(this::mapToDepositDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all active sagas
     */
    @GetMapping("/active")
    public ResponseEntity<SagaListResponse> getActiveSagas() {
        log.info("Received request to get all active sagas");
        
        List<DepositSagaState> activeSagas = depositSagaService.findActiveSagas();
        
        SagaListResponse response = new SagaListResponse();
        response.setItems(activeSagas.stream().map(this::mapToDepositDto).collect(Collectors.toList()));
        response.setCount(activeSagas.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Force a timeout check
     */
    @PostMapping("/check-timeouts")
    public ResponseEntity<String> checkTimeouts() {
        log.info("Received request to check for timeouts");
        
        depositSagaService.checkForTimeouts();
        
        return ResponseEntity.ok("Timeout check initiated");
    }
    
    /**
     * Map a saga state entity to a Deposit DTO
     */
    private DepositSagaDto mapToDepositDto(DepositSagaState saga) {
        DepositSagaDto dto = new DepositSagaDto();
        dto.setSagaId(saga.getSagaId());
        dto.setUserId(saga.getUserId());
        dto.setAccountId(saga.getAccountId());
        dto.setAmount(saga.getAmount());
        dto.setCurrency(saga.getCurrency());
        dto.setPaymentMethodId(saga.getPaymentMethodId());
        dto.setTransactionId(saga.getTransactionId());
        
        if (saga.getCurrentStep() != null) {
            dto.setCurrentStep(saga.getCurrentStep().name());
            dto.setCurrentStepNumber(saga.getCurrentStep().getStepNumber());
        }
        
        dto.setStatus(saga.getStatus().name());
        dto.setCompletedSteps(saga.getCompletedSteps());
        dto.setStartTime(saga.getStartTime());
        dto.setEndTime(saga.getEndTime());
        dto.setLastUpdatedTime(saga.getLastUpdatedTime());
        
        dto.setFailureReason(saga.getFailureReason());
        dto.setRetryCount(saga.getRetryCount());
        dto.setMaxRetries(saga.getMaxRetries());
        
        // Filter events to a reasonable number (last 10)
        if (saga.getSagaEvents() != null && !saga.getSagaEvents().isEmpty()) {
            int eventsSize = saga.getSagaEvents().size();
            int startIndex = Math.max(0, eventsSize - 10);
            dto.setRecentEvents(saga.getSagaEvents().subList(startIndex, eventsSize));
        }
        
        return dto;
    }
}
