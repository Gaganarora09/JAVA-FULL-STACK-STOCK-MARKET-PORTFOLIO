package com.portfoliopro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    private String sector;

    @Column(precision = 19, scale = 4)
    private BigDecimal demoEarningsPerShare;

    @Column(precision = 24, scale = 2)
    private BigDecimal demoMarketCapitalization;

    @Column(precision = 10, scale = 6)
    private BigDecimal demoDividendYield;

    private Instant priceUpdatedAt = Instant.now();

    @Column(length = 30)
    private String priceSource = "SIMULATED_DEMO";
}
