package com.stocktrading.orderservice.payload.response;

import com.stocktrading.orderservice.common.PagingResponse;
import com.stocktrading.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetOrdersResponse {
    private List<Order> orders;
    private PagingResponse paging;
}
