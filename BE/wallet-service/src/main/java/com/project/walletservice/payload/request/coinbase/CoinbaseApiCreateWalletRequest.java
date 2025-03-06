package com.project.walletservice.payload.request.coinbase;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoinbaseApiCreateWalletRequest {
    private String network_id;
    private boolean use_server_signer;
}
