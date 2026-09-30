package com.portfoliopro.backend.service;

import com.portfoliopro.backend.dto.PortfolioSummaryResponse;
import com.portfoliopro.backend.dto.PortfolioAnalyticsResponse;
import com.portfoliopro.backend.entity.Trade;
import com.portfoliopro.backend.entity.Holding;
import com.portfoliopro.backend.entity.Portfolio;
import com.portfoliopro.backend.entity.User;
import com.portfoliopro.backend.repository.PortfolioRepository;
import com.portfoliopro.backend.repository.UserRepository;
import com.portfoliopro.backend.repository.TradeRepository;
import com.portfoliopro.backend.repository.PortfolioSnapshotRepository;
import com.portfoliopro.backend.entity.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Long userId = user.getId();

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User has no portfolio: " + userId));

        List<PortfolioSummaryResponse.HoldingLine> lines = portfolio.getHoldings().stream()
                .map(this::toHoldingLine)
                .collect(Collectors.toList());

        BigDecimal totalMarketValue = lines.stream()
                .map(PortfolioSummaryResponse.HoldingLine::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAccountValue = user.getCashBalance().add(totalMarketValue);
        BigDecimal totalUnrealizedGainLoss = lines.stream()
                .map(PortfolioSummaryResponse.HoldingLine::getUnrealizedGainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioSummaryResponse(
                user.getCashBalance(),
                totalMarketValue,
                totalAccountValue,
                totalUnrealizedGainLoss,
                lines
        );
    }

    @Transactional(readOnly = true)
    public PortfolioAnalyticsResponse getAnalytics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        PortfolioSummaryResponse summary = getSummary(username);
        BigDecimal investmentValue = summary.getTotalMarketValue();
        BigDecimal accountValue = summary.getTotalAccountValue();
        BigDecimal realizedGainLoss = tradeRepository.findByUserIdAndType(user.getId(), Trade.TradeType.SELL)
                .stream().map(Trade::getRealizedGainLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGainLoss = realizedGainLoss.add(summary.getTotalUnrealizedGainLoss());

        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no portfolio: " + user.getId()));
        Map<String, String> sectorsByTicker = new java.util.HashMap<>();
        portfolio.getHoldings().forEach(holding -> sectorsByTicker.put(holding.getStock().getTicker(),
                holding.getStock().getSector() == null ? "Unknown" : holding.getStock().getSector()));
        List<PortfolioAnalyticsResponse.HoldingAllocation> holdingAllocations = summary.getHoldings().stream()
                .map(line -> new PortfolioAnalyticsResponse.HoldingAllocation(line.getTicker(), line.getCompanyName(),
                        sectorsByTicker.getOrDefault(line.getTicker(), "Unknown"), line.getMarketValue(),
                        percentage(line.getMarketValue(), accountValue)))
                .toList();

        Map<String, BigDecimal> sectorValues = new TreeMap<>();
        holdingAllocations.forEach(holding -> sectorValues.merge(holding.sector(), holding.marketValue(), BigDecimal::add));
        List<PortfolioAnalyticsResponse.SectorAllocation> sectors = sectorValues.entrySet().stream()
                .map(entry -> new PortfolioAnalyticsResponse.SectorAllocation(entry.getKey(), entry.getValue(),
                        percentage(entry.getValue(), accountValue)))
                .toList();

        BigDecimal topConcentration = holdingAllocations.stream()
                .map(PortfolioAnalyticsResponse.HoldingAllocation::marketValue)
                .max(BigDecimal::compareTo)
                .map(value -> percentage(value, investmentValue))
                .orElse(BigDecimal.ZERO.setScale(2));
        String concentrationEstimate = topConcentration.compareTo(new BigDecimal("40.00")) > 0 ? "HIGH"
                : topConcentration.compareTo(new BigDecimal("20.00")) > 0 ? "MODERATE" : "LOW";

        List<PortfolioSnapshot> snapshots = snapshotRepository
                .findByUserIdAndValuationDateGreaterThanEqualOrderByValuationDateAsc(
                        user.getId(), LocalDate.now(ZoneOffset.UTC).minusDays(364));
        BigDecimal maxDrawdown = calculateMaxDrawdown(snapshots);
        BigDecimal dailyVolatility = calculateDailyVolatility(snapshots);
        String riskMetricsStatus = snapshots.size() < 3 ? "INSUFFICIENT_HISTORY" : "SIMULATED_HISTORY";

        return new PortfolioAnalyticsResponse(accountValue, summary.getCashBalance(), investmentValue,
                summary.getTotalUnrealizedGainLoss(), realizedGainLoss, totalGainLoss,
                percentage(summary.getCashBalance(), accountValue), percentage(investmentValue, accountValue),
                topConcentration, concentrationEstimate, dailyVolatility, maxDrawdown, riskMetricsStatus,
                "Concentration uses the largest stock position. Volatility and drawdown use up to 365 days of " +
                        "recorded simulated account values and are not forecasts.",
                holdingAllocations, sectors);
    }

    private BigDecimal calculateMaxDrawdown(List<PortfolioSnapshot> snapshots) {
        if (snapshots.isEmpty()) return null;
        BigDecimal peak = snapshots.getFirst().getTotalValue();
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        for (PortfolioSnapshot snapshot : snapshots) {
            BigDecimal value = snapshot.getTotalValue();
            if (value.compareTo(peak) > 0) peak = value;
            if (peak.signum() > 0) {
                BigDecimal drawdown = peak.subtract(value).multiply(new BigDecimal("100"))
                        .divide(peak, 4, java.math.RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdown) > 0) maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    private BigDecimal calculateDailyVolatility(List<PortfolioSnapshot> snapshots) {
        if (snapshots.size() < 3) return null;
        List<Double> returns = new java.util.ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            BigDecimal previous = snapshots.get(i - 1).getTotalValue();
            if (previous.signum() == 0) continue;
            double dailyReturn = snapshots.get(i).getTotalValue().subtract(previous)
                    .divide(previous, 8, java.math.RoundingMode.HALF_UP).doubleValue();
            returns.add(dailyReturn);
        }
        if (returns.size() < 2) return null;
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(value -> Math.pow(value - mean, 2)).sum()
                / (returns.size() - 1);
        return BigDecimal.valueOf(Math.sqrt(variance) * 100).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return part.multiply(new BigDecimal("100")).divide(total, 2, java.math.RoundingMode.HALF_UP);
    }

    private PortfolioSummaryResponse.HoldingLine toHoldingLine(Holding holding) {
        BigDecimal currentPrice = holding.getStock().getCurrentPrice();
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal costBasisTotal = holding.getAverageCostBasis().multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal unrealizedGainLoss = marketValue.subtract(costBasisTotal);

        return new PortfolioSummaryResponse.HoldingLine(
                holding.getStock().getTicker(),
                holding.getStock().getCompanyName(),
                holding.getQuantity(),
                holding.getAverageCostBasis(),
                currentPrice,
                marketValue,
                unrealizedGainLoss
        );
    }
}
