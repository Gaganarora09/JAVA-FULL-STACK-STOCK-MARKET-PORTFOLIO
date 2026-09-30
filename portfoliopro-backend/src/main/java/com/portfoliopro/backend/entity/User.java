package com.portfoliopro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt-encoded password; never serialize this entity in API responses.
    @Column(nullable = false)
    private String passwordHash;

    // Cash balance available for trading
    @Column(nullable = false)
    private BigDecimal cashBalance = new BigDecimal("100000.00");

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Portfolio portfolio;
}
