package com.stocktrading.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for saga list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaListResponse {
    private List<com.stocktrading.kafka.controller.dto.DepositSagaDto> sagas;
    private int count;
}
