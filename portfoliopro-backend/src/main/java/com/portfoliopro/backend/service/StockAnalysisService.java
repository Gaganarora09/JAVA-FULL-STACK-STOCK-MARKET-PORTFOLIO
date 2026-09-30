package com.portfoliopro.backend.service;

import com.portfoliopro.backend.dto.StockTechnicalAnalysisResponse;
import com.portfoliopro.backend.entity.Stock;
import com.portfoliopro.backend.entity.StockDailyPrice;
import com.portfoliopro.backend.repository.StockDailyPriceRepository;
import com.portfoliopro.backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAnalysisService {
    private final StockRepository stockRepository;
    private final StockDailyPriceRepository stockDailyPriceRepository;

    @Transactional(readOnly = true)
    public StockTechnicalAnalysisResponse technicalAnalysis(String ticker, int requestedDays) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker));
        int days = Math.max(1, Math.min(requestedDays, 365));
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L);
        List<StockDailyPrice> points = stockDailyPriceRepository
                .findByStockIdAndTradingDateGreaterThanEqualOrderByTradingDateAsc(stock.getId(), start);
        List<BigDecimal> closes = points.stream().map(StockDailyPrice::getClosePrice).toList();

        BigDecimal sma20 = closes.size() < 20 ? null
                : average(closes.subList(closes.size() - 20, closes.size()));
        BigDecimal rsi14 = closes.size() < 15 ? null : calculateRsi14(closes);
        BigDecimal latest = closes.isEmpty() ? stock.getCurrentPrice() : closes.get(closes.size() - 1);
        List<StockTechnicalAnalysisResponse.PricePoint> history = points.stream()
                .map(point -> new StockTechnicalAnalysisResponse.PricePoint(
                        point.getTradingDate(), point.getClosePrice()))
                .toList();

        return new StockTechnicalAnalysisResponse(stock.getTicker(), points.size(), latest, sma20, rsi14,
                "SIMULATED_DEMO", history);
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRsi14(List<BigDecimal> closes) {
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        int start = closes.size() - 14;
        for (int i = start; i < closes.size(); i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            if (change.signum() > 0) gains = gains.add(change);
            else losses = losses.add(change.abs());
        }
        if (losses.signum() == 0) return gains.signum() == 0
                ? new BigDecimal("50.00") : new BigDecimal("100.00");
        BigDecimal relativeStrength = gains.divide(losses, 8, RoundingMode.HALF_UP);
        return new BigDecimal("100").subtract(new BigDecimal("100").divide(
                BigDecimal.ONE.add(relativeStrength), 2, RoundingMode.HALF_UP));
    }
}
