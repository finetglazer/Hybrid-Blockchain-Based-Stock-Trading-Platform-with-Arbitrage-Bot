package com.stocktrading.kafka.controller;

import com.stocktrading.kafka.dto.OrderSellSagaDto;
import com.stocktrading.kafka.dto.OrderSellSagaRequest;
import com.stocktrading.kafka.dto.SagaListResponse;
import com.stocktrading.kafka.model.OrderSellSagaState;
import com.stocktrading.kafka.service.OrderSellSagaService;
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
 * REST controller for order sell saga management
 */
@Slf4j
@RestController
@RequestMapping("sagas/api/v1/orders/sell")
@RequiredArgsConstructor
public class OrderSellSagaController {

    private final OrderSellSagaService orderSellSagaService;

    /**
     * Start a new order sell saga
     */
    @PostMapping
    public ResponseEntity<OrderSellSagaDto> startOrderSellSaga(@Valid @RequestBody OrderSellSagaRequest request) {
        log.info("Received request to start order sell saga for account: {}, stock: {}",
                request.getAccountId(), request.getStockSymbol());

        OrderSellSagaState saga = orderSellSagaService.startSaga(
                request.getUserId(),
                request.getAccountId(),
                request.getStockSymbol(),
                request.getQuantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saga));
    }

    /**
     * Get a saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<OrderSellSagaDto> getSaga(@PathVariable String sagaId) {
        log.info("Received request to get order sell saga: {}", sagaId);

        Optional<OrderSellSagaState> optionalSaga = orderSellSagaService.findById(sagaId);

        return optionalSaga
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active order sell sagas
     */
    @GetMapping("/active")
    public ResponseEntity<SagaListResponse<OrderSellSagaDto>> getActiveSagas() {
        log.info("Received request to get all active order sell sagas");

        List<OrderSellSagaState> activeSagas = orderSellSagaService.findActiveSagas();

        SagaListResponse<OrderSellSagaDto> response = new SagaListResponse<>();
        response.setItems(activeSagas.stream().map(this::mapToDto).collect(Collectors.toList()));
        response.setCount(activeSagas.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's order sell sagas
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<SagaListResponse<OrderSellSagaDto>> getUserSagas(@PathVariable String userId) {
        log.info("Received request to get order sell sagas for user: {}", userId);

        List<OrderSellSagaState> userSagas = orderSellSagaService.findByUserId(userId);

        SagaListResponse<OrderSellSagaDto> response = new SagaListResponse<>();
        response.setItems(userSagas.stream().map(this::mapToDto).collect(Collectors.toList()));
        response.setCount(userSagas.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order by user request
     */
    @PostMapping("/{sagaId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String sagaId) {
        log.info("Received request to cancel order sell saga: {}", sagaId);

        try {
            OrderSellSagaState saga = orderSellSagaService.cancelOrderByUser(sagaId);
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
     * Force a timeout check
     */
    @PostMapping("/check-timeouts")
    public ResponseEntity<String> checkTimeouts() {
        log.info("Received request to check for timeouts");

        orderSellSagaService.checkForTimeouts();

        return ResponseEntity.ok("Timeout check initiated");
    }

    /**
     * Map a saga state entity to a DTO
     */
    private OrderSellSagaDto mapToDto(OrderSellSagaState saga) {
        OrderSellSagaDto dto = new OrderSellSagaDto();
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
        dto.setReservedQuantity(saga.getReservedQuantity());
        dto.setReservationId(saga.getReservationId());
        dto.setSettlementAmount(saga.getSettlementAmount());

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