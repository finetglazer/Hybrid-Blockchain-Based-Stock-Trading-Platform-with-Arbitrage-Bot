package com.project.walletservice.service;

import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.payload.request.client.CreateWalletRequest;

public interface CoinbaseWalletService {
     BaseResponse<?> createWallet(String userId, CreateWalletRequest createWalletRequest);
}
