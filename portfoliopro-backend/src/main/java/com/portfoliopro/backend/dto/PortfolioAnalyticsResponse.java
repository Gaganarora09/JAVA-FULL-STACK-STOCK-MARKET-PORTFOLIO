package com.portfoliopro.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAnalyticsResponse(
        BigDecimal totalAccountValue,
        BigDecimal cashBalance,
        BigDecimal investmentMarketValue,
        BigDecimal unrealizedGainLoss,
        BigDecimal realizedGainLoss,
        BigDecimal totalGainLoss,
        BigDecimal cashAllocationPercent,
        BigDecimal stockAllocationPercent,
        BigDecimal topHoldingConcentrationPercent,
        String concentrationRiskEstimate,
        BigDecimal dailyVolatilityPercent,
        BigDecimal maxDrawdownPercent,
        String riskMetricsStatus,
        String riskDescription,
        List<HoldingAllocation> holdings,
        List<SectorAllocation> sectors) {

    public record HoldingAllocation(String ticker, String companyName, String sector,
                                    BigDecimal marketValue, BigDecimal allocationPercent) {}

    public record SectorAllocation(String sector, BigDecimal marketValue, BigDecimal allocationPercent) {}
}
