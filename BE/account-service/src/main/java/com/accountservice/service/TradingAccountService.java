package com.accountservice.service;

import com.accountservice.common.BaseResponse;
import com.accountservice.payload.request.client.CreateTradingAccountRequest;
import com.accountservice.payload.request.client.GetBalanceHistoryRequest;
import com.accountservice.payload.request.client.GetUserAccountsRequest;
import com.accountservice.payload.request.client.UpdateTradingAccountRequest;
import com.accountservice.payload.response.internal.HasTradingAccountAndPaymentMethodResponse;

public interface TradingAccountService {
    BaseResponse<?> createTradingAccount(String userId, CreateTradingAccountRequest createTradingAccountRequest);
    BaseResponse<?> getAccountDetails(String accountId);
    BaseResponse<?> getUserAccounts(GetUserAccountsRequest getUserAccountsRequest);
    BaseResponse<?> updateTradingAccount(UpdateTradingAccountRequest updateTradingAccountRequest);
    BaseResponse<?> getBalanceHistory(GetBalanceHistoryRequest getBalanceHistoryRequest);
    HasTradingAccountAndPaymentMethodResponse hasAccountAndPaymentMethod(String userId);
    BaseResponse<?> getUserAccountNames(String userId);
}
