package com.project.walletservice.payload.request.coinbase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinbaseApiExchangeCodeRequest {
    private String grant_type;
    private String code;
    private String client_id;
    private String client_secret;
    private String redirect_uri;
}
