package com.stocktrading.orderservice.service;

import com.stocktrading.orderservice.common.BaseResponse;
import com.stocktrading.orderservice.payload.request.GetOrderRequest;

public interface OrderHistoryService {
    BaseResponse<?> getOrders(GetOrderRequest getOrderRequest);
    BaseResponse<?> getOrderDetails(String orderId);
}
