package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record StockFundamentalsResponse(
        String ticker,
        String companyName,
        BigDecimal simulatedPrice,
        BigDecimal earningsPerShare,
        BigDecimal priceToEarnings,
        BigDecimal marketCapitalization,
        BigDecimal dividendYield,
        String dataSource,
        Instant updatedAt) {

    public static StockFundamentalsResponse from(Stock stock) {
        BigDecimal eps = stock.getDemoEarningsPerShare();
        BigDecimal pe = eps == null || eps.signum() <= 0 ? null
                : stock.getCurrentPrice().divide(eps, 2, RoundingMode.HALF_UP);
        return new StockFundamentalsResponse(stock.getTicker(), stock.getCompanyName(), stock.getCurrentPrice(),
                eps, pe, stock.getDemoMarketCapitalization(), stock.getDemoDividendYield(),
                "SIMULATED_DEMO", stock.getPriceUpdatedAt());
    }
}
