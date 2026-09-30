package com.portfoliopro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "holding", uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "stock_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private Integer quantity;

    // Weighted average price paid per share, for P&L calculations later
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCostBasis;
}
