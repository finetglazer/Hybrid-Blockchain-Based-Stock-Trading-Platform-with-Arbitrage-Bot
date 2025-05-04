package com.stocktrading.orderservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetOrderRequest {
    private String userId;
    private List<String> accountIds;
    private String startDate;
    private String endDate;
    private List<String> timeInForces;
    private List<String> orderTypes;
    private List<String> statuses;
    private List<String> sides;
    private List<String> stockSymbols;
    private int page;
    private int size;
}
