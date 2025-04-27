package com.stocktrading.marketdata.service.impl;

import com.stocktrading.marketdata.common.BaseResponse;
import com.stocktrading.marketdata.common.Const;
import com.stocktrading.marketdata.model.Portfolio;
import com.stocktrading.marketdata.repository.PortfolioRepository;
import com.stocktrading.marketdata.service.PortfolioService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {
    private final PortfolioRepository portfolioRepository;

    @Override
    public BaseResponse<?> getPortfolio(String accountId, String userId) {
        Portfolio portfolio = portfolioRepository.findPortfolioByAccountIdAndUserId(accountId, userId).orElse(null);
        if (portfolio == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Portfolio not found for accountId: " + accountId + " and userId: " + userId,
                ""
            );
        }
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Portfolio retrieve successfully",
            portfolio
        );
    }
}
