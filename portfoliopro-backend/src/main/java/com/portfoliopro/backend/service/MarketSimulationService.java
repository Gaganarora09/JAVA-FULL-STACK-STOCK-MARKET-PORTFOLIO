package com.portfoliopro.backend.service;

import com.portfoliopro.backend.entity.Stock;
import com.portfoliopro.backend.entity.StockDailyPrice;
import com.portfoliopro.backend.repository.StockDailyPriceRepository;
import com.portfoliopro.backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class MarketSimulationService {
    private final StockRepository stockRepository;
    private final StockDailyPriceRepository priceRepository;
    private final PortfolioSnapshotService portfolioSnapshotService;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void advanceDemoMarketDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (Stock stock : stockRepository.findAll()) {
            if (priceRepository.existsByStockIdAndTradingDate(stock.getId(), today)) continue;

            double dailyReturn = dailyReturn(stock.getTicker(), today);
            BigDecimal close = stock.getCurrentPrice().multiply(BigDecimal.valueOf(1.0 + dailyReturn))
                    .setScale(4, RoundingMode.HALF_UP);
            stock.setCurrentPrice(close);
            stock.setPriceUpdatedAt(Instant.now());
            stockRepository.save(stock);

            StockDailyPrice point = new StockDailyPrice();
            point.setStock(stock);
            point.setTradingDate(today);
            point.setClosePrice(close);
            priceRepository.save(point);
        }
        portfolioSnapshotService.captureAllDaily();
    }

    private double dailyReturn(String ticker, LocalDate date) {
        long mixed = 31L * ticker.hashCode() + date.toEpochDay() + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        double unitInterval = (mixed >>> 11) * 0x1.0p-53;
        return (unitInterval * 0.04) - 0.02;
    }
}
