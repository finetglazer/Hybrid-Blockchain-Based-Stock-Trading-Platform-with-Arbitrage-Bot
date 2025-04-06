package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserAccountsRequest {
    private String userId;
    private String status;
    private Integer page;
    private Integer size;
}
