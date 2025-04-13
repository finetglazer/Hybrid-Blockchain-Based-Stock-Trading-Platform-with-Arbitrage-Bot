package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsRequest {
    private String userId;
    private List<String> accountIds;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private List<String> types;
    private List<String> statuses;
    private List<String> paymentMethodIds;
    private Integer page;
    private Integer size;
}
