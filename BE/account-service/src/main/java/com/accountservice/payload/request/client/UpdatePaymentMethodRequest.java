package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePaymentMethodRequest {
    private String nickname;
    private String status;
    private boolean setAsDefault;
    private UpdatePaymentMethodMetadataRequest metadata;
}
