package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.StockResponse;
import com.portfoliopro.backend.dto.StockFundamentalsResponse;
import com.portfoliopro.backend.dto.StockTechnicalAnalysisResponse;
import com.portfoliopro.backend.dto.StockSearchResponse;
import com.portfoliopro.backend.service.MarketDataService;
import com.portfoliopro.backend.repository.StockRepository;
import com.portfoliopro.backend.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockRepository stockRepository;
    private final StockAnalysisService stockAnalysisService;
    private final MarketDataService marketDataService;

    @GetMapping
    public List<StockResponse> listStocks() {
        return stockRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(com.portfoliopro.backend.entity.Stock::getTicker))
                .map(StockResponse::from).toList();
    }

    @GetMapping("/search")
    public List<StockSearchResponse> searchStocks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sector) {
        String query = normalize(q);
        String normalizedSector = normalize(sector);
        List<StockSearchResponse> results = new java.util.ArrayList<>(stockRepository.search(query, normalizedSector)
                .stream().map(StockSearchResponse::from).toList());
        if (query != null && normalizedSector == null && marketDataService.configured()) {
            marketDataService.searchTickers(query).stream()
                    .map(match -> StockSearchResponse.external(match.ticker(), match.companyName(), match.primaryExchange()))
                    .forEach(results::add);
        }
        return results;
    }

    @PostMapping("/{ticker}/import")
    public StockResponse importStock(@PathVariable String ticker) {
        return StockResponse.from(marketDataService.importTicker(ticker));
    }

    @GetMapping("/sectors")
    public List<String> listSectors() {
        return stockRepository.findDistinctSectors();
    }

    @GetMapping("/{ticker}")
    public StockResponse getStock(@PathVariable String ticker) {
        return stockRepository.findByTicker(ticker.toUpperCase())
                .map(StockResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker));
    }

    @GetMapping("/{ticker}/fundamentals")
    public StockFundamentalsResponse getFundamentals(@PathVariable String ticker) {
        return stockRepository.findByTicker(ticker.toUpperCase())
                .map(StockFundamentalsResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker));
    }

    @GetMapping("/{ticker}/technical-analysis")
    public StockTechnicalAnalysisResponse getTechnicalAnalysis(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "30") int days) {
        return stockAnalysisService.technicalAnalysis(ticker, days);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
