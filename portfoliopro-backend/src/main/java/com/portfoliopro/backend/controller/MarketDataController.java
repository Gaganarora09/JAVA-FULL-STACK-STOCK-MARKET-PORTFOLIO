package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {
    private final MarketDataService marketDataService;

    @GetMapping("/status")
    public MarketDataService.Status status() { return marketDataService.status(); }

    @PostMapping("/refresh")
    public MarketDataService.RefreshResult refresh() { return marketDataService.refreshAll(); }
}
