package com.accountservice.payload.response.client;

import com.accountservice.common.PagingResponse;
import com.accountservice.model.BalanceHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetBalanceHistoryResponse {
    private List<BalanceHistory> items;
    private PagingResponse paging;
}
