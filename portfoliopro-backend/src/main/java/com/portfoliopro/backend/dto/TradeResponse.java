package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(Long id, String ticker, String companyName, Trade.TradeType type,
                            Integer quantity, BigDecimal priceAtExecution, BigDecimal realizedGainLoss,
                            Instant executedAt) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.getId(), trade.getStock().getTicker(),
                trade.getStock().getCompanyName(), trade.getType(), trade.getQuantity(),
                trade.getPriceAtExecution(), trade.getRealizedGainLoss(), trade.getExecutedAt());
    }
}
