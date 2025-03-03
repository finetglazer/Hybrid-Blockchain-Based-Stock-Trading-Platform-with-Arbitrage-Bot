package com.project.walletservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWalletRequest {
    private String walletName;
    private String network;
    private String signer;

    //eg
    /*
    walletName: "My Wallet",
    network: "BTC",
    signer: "My Signer"
     */
}
