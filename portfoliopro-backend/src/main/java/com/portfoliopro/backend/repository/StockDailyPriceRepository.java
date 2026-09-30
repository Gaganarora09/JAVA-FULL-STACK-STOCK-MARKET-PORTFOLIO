package com.portfoliopro.backend.repository;

import com.portfoliopro.backend.entity.StockDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockDailyPriceRepository extends JpaRepository<StockDailyPrice, Long> {
    List<StockDailyPrice> findByStockIdAndTradingDateGreaterThanEqualOrderByTradingDateAsc(
            Long stockId, LocalDate startDate);
    boolean existsByStockId(Long stockId);
    boolean existsByStockIdAndTradingDate(Long stockId, LocalDate tradingDate);
}
