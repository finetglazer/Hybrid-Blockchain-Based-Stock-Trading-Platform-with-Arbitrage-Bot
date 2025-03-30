package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetBalanceHistoryRequest {
    private String accountId;
    private String startDate;
    private String endDate;
    private Integer page;
    private Integer size;
}
