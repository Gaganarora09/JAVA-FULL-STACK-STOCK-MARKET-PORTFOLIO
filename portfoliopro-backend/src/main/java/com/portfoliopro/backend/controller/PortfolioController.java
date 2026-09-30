package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.PortfolioSummaryResponse;
import com.portfoliopro.backend.dto.PortfolioAnalyticsResponse;
import com.portfoliopro.backend.dto.PortfolioPerformanceResponse;
import com.portfoliopro.backend.entity.PortfolioSnapshot;
import com.portfoliopro.backend.repository.PortfolioSnapshotRepository;
import com.portfoliopro.backend.repository.UserRepository;
import com.portfoliopro.backend.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public PortfolioSummaryResponse getSummary(Authentication authentication) {
        return portfolioService.getSummary(authentication.getName());
    }

    @GetMapping("/analytics")
    public PortfolioAnalyticsResponse getAnalytics(Authentication authentication) {
        return portfolioService.getAnalytics(authentication.getName());
    }

    @GetMapping("/performance")
    public List<PortfolioPerformanceResponse> getPerformance(
            Authentication authentication,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer days) {
        Long userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        List<PortfolioSnapshot> snapshots;
        if (days != null) {
            int boundedDays = Math.max(1, Math.min(days, 365));
            LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(boundedDays - 1L);
            snapshots = snapshotRepository
                    .findByUserIdAndValuationDateGreaterThanEqualOrderByValuationDateAsc(userId, startDate);
        } else {
            String selectedRange = range == null ? "month" : range.toLowerCase();
            snapshots = switch (selectedRange) {
                case "week" -> performanceSince(userId, 7);
                case "month" -> performanceSince(userId, 30);
                case "year" -> performanceSince(userId, 365);
                case "all" -> snapshotRepository.findByUserIdOrderByValuationDateAsc(userId);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "range must be one of: week, month, year, all");
            };
        }
        return snapshots.stream().map(PortfolioPerformanceResponse::from).toList();
    }

    private List<PortfolioSnapshot> performanceSince(Long userId, int days) {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L);
        return snapshotRepository
                .findByUserIdAndValuationDateGreaterThanEqualOrderByValuationDateAsc(userId, startDate);
    }
}
