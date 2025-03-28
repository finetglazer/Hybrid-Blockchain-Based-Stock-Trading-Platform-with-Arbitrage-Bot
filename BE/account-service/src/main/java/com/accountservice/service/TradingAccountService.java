package com.accountservice.service;

import com.accountservice.common.BaseResponse;
import com.accountservice.payload.request.client.CreateTradingAccountRequest;
import com.accountservice.payload.request.client.GetUserAccountsRequest;
import com.accountservice.payload.request.client.UpdateTradingAccountRequest;

public interface TradingAccountService {
    BaseResponse<?> createTradingAccount(String userId, CreateTradingAccountRequest createTradingAccountRequest);
    BaseResponse<?> getAccountDetails(String accountId);
    BaseResponse<?> getUserAccounts(GetUserAccountsRequest getUserAccountsRequest);
    BaseResponse<?> updateTradingAccount(UpdateTradingAccountRequest updateTradingAccountRequest);
}
