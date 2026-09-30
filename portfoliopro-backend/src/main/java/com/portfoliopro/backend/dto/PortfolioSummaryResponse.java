package com.portfoliopro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PortfolioSummaryResponse {

    private BigDecimal cashBalance;
    private BigDecimal totalMarketValue;
    private BigDecimal totalAccountValue; // cash + market value
    private BigDecimal totalUnrealizedGainLoss;
    private List<HoldingLine> holdings;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class HoldingLine {
        private String ticker;
        private String companyName;
        private Integer quantity;
        private BigDecimal averageCostBasis;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal unrealizedGainLoss;
    }
}
