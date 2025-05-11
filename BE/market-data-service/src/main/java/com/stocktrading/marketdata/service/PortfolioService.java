package com.stocktrading.marketdata.service;

import com.stocktrading.marketdata.common.BaseResponse;

public interface PortfolioService {
    BaseResponse<?> getPortfolio(String userId, String accountId);
    BaseResponse<?> getGeneralPortfolio(String userId);
}
