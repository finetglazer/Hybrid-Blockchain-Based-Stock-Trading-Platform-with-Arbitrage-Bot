package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsRequest {
    private String accountId;
    private String startDate;
    private String endDate;
    private List<String> types;
    private List<String> statuses;
    private Integer page;
    private Integer size;
}
