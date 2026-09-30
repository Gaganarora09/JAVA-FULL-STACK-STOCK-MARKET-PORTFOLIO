package com.portfoliopro.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockTechnicalAnalysisResponse(
        String ticker,
        int periodDays,
        BigDecimal latestClose,
        BigDecimal sma20,
        BigDecimal rsi14,
        String dataSource,
        List<PricePoint> history) {
    public record PricePoint(LocalDate date, BigDecimal close) { }
}
