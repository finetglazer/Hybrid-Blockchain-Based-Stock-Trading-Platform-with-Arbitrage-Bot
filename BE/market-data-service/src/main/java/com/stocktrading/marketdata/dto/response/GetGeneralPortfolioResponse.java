package com.stocktrading.marketdata.dto.response;

import com.stocktrading.marketdata.model.PositionInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetGeneralPortfolioResponse {
    private String userId;
    private String name;
    private List<PositionInfo> positions;
}
