package com.portfoliopro.backend.service;

import com.portfoliopro.backend.entity.Portfolio;
import com.portfoliopro.backend.entity.PortfolioSnapshot;
import com.portfoliopro.backend.entity.User;
import com.portfoliopro.backend.repository.PortfolioRepository;
import com.portfoliopro.backend.repository.PortfolioSnapshotRepository;
import com.portfoliopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @Transactional
    public void capture(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no portfolio: " + user.getId()));

        BigDecimal investedValue = portfolio.getHoldings().stream()
                .map(holding -> holding.getStock().getCurrentPrice()
                        .multiply(BigDecimal.valueOf(holding.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        PortfolioSnapshot snapshot = snapshotRepository.findByUserIdAndValuationDate(user.getId(), today)
                .orElseGet(PortfolioSnapshot::new);
        snapshot.setUser(user);
        snapshot.setValuationDate(today);
        snapshot.setCashBalance(user.getCashBalance());
        snapshot.setInvestedValue(investedValue);
        snapshot.setTotalValue(user.getCashBalance().add(investedValue));
        snapshotRepository.save(snapshot);
    }

    @Transactional
    public void captureAllDaily() {
        userRepository.findAll().forEach(user -> capture(user.getUsername()));
    }
}
