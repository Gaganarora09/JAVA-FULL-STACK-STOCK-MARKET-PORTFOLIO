package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.WatchlistEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistItemResponse(String ticker, String companyName, String sector,
                                    BigDecimal currentPrice, Instant priceUpdatedAt, Instant addedAt) {
    public static WatchlistItemResponse from(WatchlistEntry entry) {
        var stock = entry.getStock();
        return new WatchlistItemResponse(stock.getTicker(), stock.getCompanyName(), stock.getSector(),
                stock.getCurrentPrice(), stock.getPriceUpdatedAt(), entry.getAddedAt());
    }
}
