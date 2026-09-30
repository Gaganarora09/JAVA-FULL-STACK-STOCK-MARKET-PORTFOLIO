package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Stock;

import java.math.BigDecimal;
import java.time.Instant;

public record StockResponse(String ticker, String companyName, BigDecimal simulatedPrice,
                            String priceType, String sector, Instant priceUpdatedAt) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(stock.getTicker(), stock.getCompanyName(), stock.getCurrentPrice(),
                "SIMULATED_DEMO", stock.getSector(), stock.getPriceUpdatedAt());
    }
}
