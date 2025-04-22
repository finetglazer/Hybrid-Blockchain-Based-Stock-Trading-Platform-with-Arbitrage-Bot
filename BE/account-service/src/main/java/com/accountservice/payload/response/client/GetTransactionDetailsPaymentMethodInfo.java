package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionDetailsPaymentMethodInfo {
    private String id;
    private String nickname;
    private String maskedNumber;
    private String type;
    private String status;
    private Map<String, Object> metadata;
}
