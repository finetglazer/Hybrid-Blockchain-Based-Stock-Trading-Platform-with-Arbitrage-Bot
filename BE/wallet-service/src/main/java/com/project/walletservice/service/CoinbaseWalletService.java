package com.project.walletservice.service;

import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.payload.response.CreateWalletResponse;

public interface CoinbaseWalletService {
    CreateWalletResponse createWallet(String userId, CreateWalletRequest createWalletRequest);
}
