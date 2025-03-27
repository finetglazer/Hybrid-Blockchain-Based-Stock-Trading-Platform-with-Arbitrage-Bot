package com.project.walletservice.payload.request.coinbase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinbaseApiCreateWalletRequest {
    private String network_id;
    private boolean use_server_signer;
}
