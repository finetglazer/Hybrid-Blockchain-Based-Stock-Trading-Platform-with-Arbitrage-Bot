package com.accountservice.payload.response.client;

import com.accountservice.common.PagingResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserAccountsResponse {
    private List<RetrieveAccountInfoResponse> items;
    private PagingResponse paging;
}
