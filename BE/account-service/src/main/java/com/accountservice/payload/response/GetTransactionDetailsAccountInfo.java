package com.accountservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionDetailsAccountInfo {
    private String id;
    private String nickname;
    private String accountNumber;
    private String status;
}
