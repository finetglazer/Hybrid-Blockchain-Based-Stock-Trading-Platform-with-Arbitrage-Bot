package com.project.walletservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeatureSet {
    private boolean faucet; // Whether a network supports a faucet
    private boolean server_signer;  // Whether the network supports Server-Signers
    private boolean transfer;   // Whether the network supports transfers
    private boolean trade;  // Whether the network supports trading
    private boolean stake;  // Whether the network supports staking
    private boolean gasless_send;   // Whether the network supports gasless sends
}
