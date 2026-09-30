package com.portfoliopro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_daily_price", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_price_date", columnNames = {"stock_id", "trading_date"}))
@Getter
@Setter
@NoArgsConstructor
public class StockDailyPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "trading_date", nullable = false)
    private LocalDate tradingDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;
}
