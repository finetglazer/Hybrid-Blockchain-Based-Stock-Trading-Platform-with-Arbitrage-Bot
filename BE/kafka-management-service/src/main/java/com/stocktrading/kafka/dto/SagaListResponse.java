package com.stocktrading.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic response DTO for saga lists
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaListResponse<T> {
    private List<T> items;
    private int count;
}