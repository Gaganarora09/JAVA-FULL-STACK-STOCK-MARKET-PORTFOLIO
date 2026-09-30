package com.portfoliopro.backend.repository;

import com.portfoliopro.backend.entity.WatchlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {
    List<WatchlistEntry> findByUserIdOrderByAddedAtDesc(Long userId);
    Optional<WatchlistEntry> findByUserIdAndStockId(Long userId, Long stockId);
    void deleteByUserIdAndStockId(Long userId, Long stockId);
}
