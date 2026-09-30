package com.portfoliopro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_snapshot", uniqueConstraints =
        @UniqueConstraint(name = "uk_snapshot_user_date", columnNames = {"user_id", "valuation_date"}))
@Getter
@Setter
@NoArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cashBalance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal investedValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;
}
