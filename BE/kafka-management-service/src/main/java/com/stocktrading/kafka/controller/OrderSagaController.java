package com.stocktrading.kafka.controller;

import com.stocktrading.kafka.dto.OrderBuySagaDto;
import com.stocktrading.kafka.dto.OrderBuySagaRequest;
import com.stocktrading.kafka.dto.SagaListResponse;
import com.stocktrading.kafka.model.OrderBuySagaState;
import com.stocktrading.kafka.service.OrderBuySagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for order saga management
 */
@Slf4j
@RestController
@RequestMapping("sagas/api/v1/orders")
@RequiredArgsConstructor
public class OrderSagaController {

    private final OrderBuySagaService orderBuySagaService;

    /**
     * Cancel an order by user request
     */
    @PostMapping("/{sagaId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String sagaId) {
        log.info("Received request to cancel order saga: {}", sagaId);

        try {
            OrderBuySagaState saga = orderBuySagaService.cancelOrderByUser(sagaId);
            return ResponseEntity.ok(mapToDto(saga));
        } catch (IllegalStateException e) {
            // Order cannot be cancelled in current state
            log.warn("Cannot cancel order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            log.error("Error cancelling order saga: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "status", "error",
                            "message", "Failed to cancel order: " + e.getMessage()
                    )
            );
        }
    }

    /**
     * Start a new order buy saga
     */
    @PostMapping("/buy")
    public ResponseEntity<OrderBuySagaDto> startOrderBuySaga(@Valid @RequestBody OrderBuySagaRequest request) {
        log.info("Received request to start order buy saga for account: {}, stock: {}",
                request.getAccountId(), request.getStockSymbol());

        OrderBuySagaState saga = orderBuySagaService.startSaga(
                request.getUserId(),
                request.getAccountId(),
                request.getStockSymbol(),
                request.getOrderType(),
                request.getQuantity(),
                request.getLimitPrice(),
                request.getTimeInForce()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saga));
    }

    /**
     * Get a saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<OrderBuySagaDto> getSaga(@PathVariable String sagaId) {
        log.info("Received request to get order saga: {}", sagaId);

        Optional<OrderBuySagaState> optionalSaga = orderBuySagaService.findById(sagaId);

        return optionalSaga
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active order sagas
     */
    @GetMapping("/active")
    public ResponseEntity<SagaListResponse<OrderBuySagaDto>> getActiveSagas() {
        log.info("Received request to get all active order sagas");

        List<OrderBuySagaState> activeSagas = orderBuySagaService.findActiveSagas();

        SagaListResponse<OrderBuySagaDto> response = new SagaListResponse<>();
        response.setItems(activeSagas.stream().map(this::mapToDto).collect(Collectors.toList()));
        response.setCount(activeSagas.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's order sagas
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<SagaListResponse<OrderBuySagaDto>> getUserSagas(@PathVariable String userId) {
        log.info("Received request to get order sagas for user: {}", userId);

        List<OrderBuySagaState> userSagas = orderBuySagaService.findByUserId(userId);

        SagaListResponse<OrderBuySagaDto> response = new SagaListResponse<>();
        response.setItems(userSagas.stream().map(this::mapToDto).collect(Collectors.toList()));
        response.setCount(userSagas.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Force a timeout check
     */
    @PostMapping("/check-timeouts")
    public ResponseEntity<String> checkTimeouts() {
        log.info("Received request to check for timeouts");

        orderBuySagaService.checkForTimeouts();

        return ResponseEntity.ok("Timeout check initiated");
    }

    /**
     * Map a saga state entity to a DTO
     */
    private OrderBuySagaDto mapToDto(OrderBuySagaState saga) {
        OrderBuySagaDto dto = new OrderBuySagaDto();
        dto.setSagaId(saga.getSagaId());
        dto.setUserId(saga.getUserId());
        dto.setAccountId(saga.getAccountId());
        dto.setStockSymbol(saga.getStockSymbol());
        dto.setOrderType(saga.getOrderType());
        dto.setQuantity(saga.getQuantity());
        dto.setLimitPrice(saga.getLimitPrice());
        dto.setTimeInForce(saga.getTimeInForce());
        dto.setOrderId(saga.getOrderId());
        dto.setExecutionPrice(saga.getExecutionPrice());
        dto.setExecutedQuantity(saga.getExecutedQuantity());

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