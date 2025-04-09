package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListPaymentMethodsResponse {
    private List<RetrievePaymentMethodResponse> items;
}
