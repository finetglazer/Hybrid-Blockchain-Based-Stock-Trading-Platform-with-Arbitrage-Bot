package com.project.walletservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWalletResponse {
    private String coinbaseWalletId;
    private String message;
}
