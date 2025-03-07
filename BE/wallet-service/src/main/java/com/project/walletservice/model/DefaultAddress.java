package com.project.walletservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DefaultAddress {
    private String wallet_id;   // wallet_id but it is the ID of default address in wallet
    private String network_id;
    private String public_key;
    private String address_id;
}
