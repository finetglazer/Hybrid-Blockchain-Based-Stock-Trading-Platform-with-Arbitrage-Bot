package com.stocktrading.orderservice.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PagingResponse {
    private Integer page;
    private Integer size;
    private Long totalItems;
    private Integer totalPages;
}
