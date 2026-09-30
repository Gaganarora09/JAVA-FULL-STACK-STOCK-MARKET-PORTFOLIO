package com.portfoliopro.backend.repository;

import com.portfoliopro.backend.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    Optional<PortfolioSnapshot> findByUserIdAndValuationDate(Long userId, LocalDate valuationDate);
    List<PortfolioSnapshot> findByUserIdAndValuationDateGreaterThanEqualOrderByValuationDateAsc(
            Long userId, LocalDate startDate);
    List<PortfolioSnapshot> findByUserIdOrderByValuationDateAsc(Long userId);
}
