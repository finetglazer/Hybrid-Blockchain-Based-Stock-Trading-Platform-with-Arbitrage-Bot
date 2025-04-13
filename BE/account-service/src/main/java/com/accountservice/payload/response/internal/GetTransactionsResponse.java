package com.accountservice.payload.response.internal;

import com.accountservice.common.PagingResponse;
import com.accountservice.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsResponse {
    private List<Transaction> items;
    private PagingResponse paging;
}
