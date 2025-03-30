package com.project.userservice.client;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.payload.response.internal.HasTradingAccountAndPaymentMethodResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AccountServiceClient {
    private final RestTemplate restTemplate;

    @Value("${account.service.baseUrl}")
    private String accountServiceBaseUrl;

    public BaseResponse<HasTradingAccountAndPaymentMethodResponse> hasTradingAccountAndPaymentMethod(String userId) {
        String url = accountServiceBaseUrl + "/accounts/api/v1/internal/" + userId + "/has-account-and-payment-method";

        ResponseEntity<HasTradingAccountAndPaymentMethodResponse> response =
                restTemplate.postForEntity(url, null, HasTradingAccountAndPaymentMethodResponse.class);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Checking completed",
            response.getBody()
        );
    }

}
