package com.stocktrading.marketdata.service.impl;

import com.stocktrading.marketdata.common.BaseResponse;
import com.stocktrading.marketdata.common.Const;
import com.stocktrading.marketdata.dto.response.GetGeneralPortfolioResponse;
import com.stocktrading.marketdata.model.Portfolio;
import com.stocktrading.marketdata.model.PositionInfo;
import com.stocktrading.marketdata.repository.PortfolioRepository;
import com.stocktrading.marketdata.service.PortfolioService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

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

    @Override
    public BaseResponse<?> getGeneralPortfolio(String userId) {
        GetGeneralPortfolioResponse response = new GetGeneralPortfolioResponse();
        List<Portfolio> portfolios = portfolioRepository.getPortfoliosByUserId(userId);
        HashMap<String, PositionInfo> map = new HashMap<>();
        for (Portfolio portfolio : portfolios) {
            List<PositionInfo> positions = portfolio.getPositions();
            for (PositionInfo positionInfo : positions) {
                if (map.containsKey(positionInfo.getStockSymbol())) {
                    positionInfo.setQuantity(map.get(positionInfo.getStockSymbol()).getQuantity() + positionInfo.getQuantity());
                    map.put(positionInfo.getStockSymbol(), positionInfo);
                }
                else {
                    map.put(positionInfo.getStockSymbol(), positionInfo);
                }
            }
        }
        response.setPositions(map.values().stream().toList());
        response.setUserId(userId);
        response.setName("My portfolio");

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "General portfolio retrieved successfully",
            response
        );
    }
}
