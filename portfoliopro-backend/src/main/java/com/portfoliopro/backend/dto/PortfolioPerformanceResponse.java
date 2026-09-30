package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.PortfolioSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioPerformanceResponse(
        LocalDate date,
        BigDecimal cashBalance,
        BigDecimal investedValue,
        BigDecimal totalValue,
        String source) {
    public static PortfolioPerformanceResponse from(PortfolioSnapshot snapshot) {
        return new PortfolioPerformanceResponse(snapshot.getValuationDate(), snapshot.getCashBalance(),
                snapshot.getInvestedValue(), snapshot.getTotalValue(), "SIMULATED_DEMO_PRICES");
    }
}
