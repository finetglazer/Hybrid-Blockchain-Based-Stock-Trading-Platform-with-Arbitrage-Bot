package com.accountservice.service;

import com.accountservice.common.BaseResponse;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.payload.response.internal.GetTransactionsResponse;

public interface TransactionHistoryService {
    BaseResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest getTransactionsRequest);
    Transaction getLastTransaction(String accountId);
    BaseResponse<?> getTransactionDetails(String transactionId);
}
