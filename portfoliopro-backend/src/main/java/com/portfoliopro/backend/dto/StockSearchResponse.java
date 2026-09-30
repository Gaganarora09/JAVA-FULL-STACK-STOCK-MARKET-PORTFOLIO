package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Stock;

import java.math.BigDecimal;
import java.time.Instant;

public record StockSearchResponse(String ticker, String companyName, BigDecimal currentPrice,
                                  String priceType, String sector, Instant priceUpdatedAt,
                                  boolean inCatalogue, String primaryExchange) {
    public static StockSearchResponse from(Stock stock) {
        return new StockSearchResponse(stock.getTicker(), stock.getCompanyName(), stock.getCurrentPrice(),
                stock.getPriceSource() == null ? "SIMULATED_DEMO" : stock.getPriceSource(),
                stock.getSector(), stock.getPriceUpdatedAt(), true, null);
    }

    public static StockSearchResponse external(String ticker, String companyName, String primaryExchange) {
        return new StockSearchResponse(ticker, companyName, null, "NOT_IN_CATALOGUE", null, null,
                false, primaryExchange);
    }
}
