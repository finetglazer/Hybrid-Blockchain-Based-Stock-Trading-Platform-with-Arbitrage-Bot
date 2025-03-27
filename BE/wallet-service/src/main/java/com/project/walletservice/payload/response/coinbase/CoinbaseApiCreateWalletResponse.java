package com.project.walletservice.payload.response.coinbase;

import com.project.walletservice.model.DefaultAddress;
import com.project.walletservice.model.FeatureSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinbaseApiCreateWalletResponse {
    private String id;  // wallet_id which is server-assigned ID for the wallet
    private String network_id;
    private DefaultAddress default_address;
    private FeatureSet feature_set;
    private String server_signer_status;    // Possible values: [pending_seed_creation, active_seed]
}
