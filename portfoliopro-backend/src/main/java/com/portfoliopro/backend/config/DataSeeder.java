package com.portfoliopro.backend.config;

import com.portfoliopro.backend.entity.Stock;
import com.portfoliopro.backend.repository.StockRepository;
import com.portfoliopro.backend.repository.StockDailyPriceRepository;
import com.portfoliopro.backend.entity.StockDailyPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final StockDailyPriceRepository stockDailyPriceRepository;

    @Override
    public void run(String... args) {
        seedStock("AAPL", "Apple Inc.", "245.50", "Technology", "6.75", "3700000000000", "0.0041");
        seedStock("MSFT", "Microsoft Corporation", "430.20", "Technology", "12.10", "3200000000000", "0.0068");
        seedStock("GOOGL", "Alphabet Inc.", "175.80", "Technology", "8.50", "2100000000000", "0.0045");
        seedStock("TSLA", "Tesla Inc.", "265.40", "Consumer Discretionary", "3.60", "850000000000", "0.0000");
        seedStock("JPM", "JPMorgan Chase & Co.", "215.90", "Financials", "17.00", "620000000000", "0.0220");

    }

    private void seedStock(String ticker, String companyName, String price, String sector,
                           String eps, String marketCap, String dividendYield) {
        Stock stock = stockRepository.findByTicker(ticker).orElseGet(Stock::new);
        if (stock.getTicker() == null) {
            stock.setTicker(ticker);
            stock.setCompanyName(companyName);
            stock.setCurrentPrice(new BigDecimal(price));
            stock.setSector(sector);
        }
        if (stock.getDemoEarningsPerShare() == null) stock.setDemoEarningsPerShare(new BigDecimal(eps));
        if (stock.getDemoMarketCapitalization() == null) stock.setDemoMarketCapitalization(new BigDecimal(marketCap));
        if (stock.getDemoDividendYield() == null) stock.setDemoDividendYield(new BigDecimal(dividendYield));
        stock = stockRepository.save(stock);
        seedDemoHistory(stock);
    }

    private void seedDemoHistory(Stock stock) {
        if (stockDailyPriceRepository.existsByStockId(stock.getId())) return;

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BigDecimal close = stock.getCurrentPrice();
        List<StockDailyPrice> history = new ArrayList<>();
        for (int daysAgo = 0; daysAgo < 30; daysAgo++) {
            StockDailyPrice point = new StockDailyPrice();
            point.setStock(stock);
            point.setTradingDate(today.minusDays(daysAgo));
            point.setClosePrice(close.setScale(4, java.math.RoundingMode.HALF_UP));
            history.add(point);

            long seed = 31L * stock.getTicker().hashCode() + today.minusDays(daysAgo).toEpochDay();
            double dailyChange = simulatedDailyChange(seed);
            close = close.divide(BigDecimal.valueOf(1.0 + dailyChange), 8, java.math.RoundingMode.HALF_UP);
        }
        stockDailyPriceRepository.saveAll(history);
    }

    private double simulatedDailyChange(long seed) {
        long mixed = seed + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        double unitInterval = (mixed >>> 11) * 0x1.0p-53;
        return (unitInterval * 0.04) - 0.02;
    }
}
