package com.project.walletservice.service.impl;

import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.service.CoinbaseWalletService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class CoinbaseWalletServiceImpl implements CoinbaseWalletService {

    @Override
    public BaseResponse<?> createWallet(String userId, CreateWalletRequest createWalletRequest) {
        // Get valid Coinbase access token for this user

    }
}
