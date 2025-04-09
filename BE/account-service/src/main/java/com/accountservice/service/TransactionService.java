package com.accountservice.service;

import com.accountservice.common.BaseResponse;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.GetTransactionsRequest;

import java.util.List;

public interface TransactionService {
    BaseResponse<List<Transaction>> getTransactions(GetTransactionsRequest getTransactionsRequest);
    Transaction getLastTransaction(String accountId);
}
