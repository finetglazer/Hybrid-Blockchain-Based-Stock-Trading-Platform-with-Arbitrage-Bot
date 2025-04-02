package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListPaymentMethodsRequest {
    private String userId;
    private List<String> types;
    private List<String> statuses;
}
