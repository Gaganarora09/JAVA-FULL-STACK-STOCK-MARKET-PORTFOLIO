package com.portfoliopro.backend.repository;

import com.portfoliopro.backend.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);
    List<Trade> findByUserIdAndType(Long userId, Trade.TradeType type);
}
