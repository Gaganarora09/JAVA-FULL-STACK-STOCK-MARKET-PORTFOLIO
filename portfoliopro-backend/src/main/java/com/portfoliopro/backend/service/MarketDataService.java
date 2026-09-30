package com.portfoliopro.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.portfoliopro.backend.entity.Stock;
import com.portfoliopro.backend.entity.StockDailyPrice;
import com.portfoliopro.backend.repository.StockDailyPriceRepository;
import com.portfoliopro.backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("America/New_York");
    private static final Pattern TICKER_PATTERN = Pattern.compile("[A-Z0-9.\\-]{1,10}");

    private final StockRepository stockRepository;
    private final StockDailyPriceRepository priceRepository;
    private final PortfolioSnapshotService snapshotService;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.market-data.api-key:}") private String apiKey;
    @Value("${app.market-data.base-url:https://api.massive.com}") private String baseUrl;
    private volatile Instant lastSuccessfulRefresh;

    public record RefreshResult(String provider, int updatedStocks, int failedStocks, Instant refreshedAt) { }
    public record Status(boolean configured, String provider, String frequency, Instant lastSuccessfulRefresh) { }
    public record TickerMatch(String ticker, String companyName, String primaryExchange) { }

    public Status status() {
        Instant persistedRefresh = stockRepository.findAll().stream()
                .filter(stock -> "MASSIVE_EOD".equals(stock.getPriceSource()))
                .map(Stock::getPriceUpdatedAt).filter(java.util.Objects::nonNull)
                .max(Instant::compareTo).orElse(null);
        return new Status(configured(), "Massive", "End-of-day daily bars",
                lastSuccessfulRefresh == null ? persistedRefresh : lastSuccessfulRefresh);
    }

    public boolean configured() { return apiKey != null && !apiKey.isBlank(); }

    public List<TickerMatch> searchTickers(String query) {
        if (!configured() || query == null || query.trim().length() < 2) return List.of();
        var uri = UriComponentsBuilder.fromUriString(baseUrl).path("/v3/reference/tickers")
                .queryParam("market", "stocks").queryParam("type", "CS").queryParam("active", "true")
                .queryParam("search", query.trim()).queryParam("limit", 20)
                .queryParam("sort", "ticker").queryParam("apiKey", apiKey)
                .build().encode().toUri();
        try {
            JsonNode response = restClientBuilder.build().get().uri(uri).retrieve().body(JsonNode.class);
            JsonNode results = response == null ? null : response.path("results");
            if (results == null || !results.isArray()) return List.of();
            Set<String> existing = stockRepository.findAll().stream().map(Stock::getTicker)
                    .collect(java.util.stream.Collectors.toSet());
            List<TickerMatch> matches = new ArrayList<>();
            for (JsonNode result : results) {
                String ticker = result.path("ticker").asText("").toUpperCase(Locale.ROOT);
                String name = result.path("name").asText("");
                if (ticker.isBlank() || ticker.length() > 10 || name.isBlank() || existing.contains(ticker)) continue;
                matches.add(new TickerMatch(ticker, name, result.path("primary_exchange").asText(null)));
            }
            return matches;
        } catch (RestClientException exception) {
            log.warn("Massive ticker search failed ({})", exception.getClass().getSimpleName());
            return List.of();
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public Stock importTicker(String requestedTicker) {
        if (!configured()) throw new IllegalStateException("Massive market data is not configured.");
        String ticker = requestedTicker == null ? "" : requestedTicker.trim().toUpperCase(Locale.ROOT);
        if (!TICKER_PATTERN.matcher(ticker).matches()) throw new IllegalArgumentException("Invalid stock ticker.");
        var existing = stockRepository.findByTicker(ticker);
        if (existing.isPresent()) return existing.get();

        var uri = UriComponentsBuilder.fromUriString(baseUrl).path("/v3/reference/tickers")
                .queryParam("market", "stocks").queryParam("active", "true")
                .queryParam("ticker", ticker).queryParam("limit", 1).queryParam("apiKey", apiKey)
                .build().encode().toUri();
        JsonNode reference = restClientBuilder.build().get().uri(uri).retrieve().body(JsonNode.class);
        JsonNode results = reference == null ? null : reference.path("results");
        JsonNode details = results != null && results.isArray() && !results.isEmpty() ? results.get(0) : null;
        if (details == null || !ticker.equalsIgnoreCase(details.path("ticker").asText())
                || !"stocks".equalsIgnoreCase(details.path("market").asText())
                || !details.path("active").asBoolean(false)) {
            throw new IllegalArgumentException("Massive did not return an active stock for ticker " + ticker + ".");
        }

        Stock stock = new Stock();
        stock.setTicker(ticker);
        stock.setCompanyName(details.path("name").asText(ticker));
        stock.setSector("Other");
        stock.setCurrentPrice(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        stock.setPriceSource("SIMULATED_DEMO");
        stock = stockRepository.saveAndFlush(stock);
        refreshStock(stock);
        return stockRepository.save(stock);
    }

    @Scheduled(cron = "0 30 22 * * MON-FRI", zone = "UTC")
    public void refreshAfterUsMarketClose() {
        if (configured()) refreshAll();
    }

    public synchronized RefreshResult refreshAll() {
        if (!configured()) throw new IllegalStateException("Add MASSIVE_API_KEY to the backend environment to enable real stock prices.");
        int updated = 0;
        int failed = 0;
        var stocks = stockRepository.findAll().stream().sorted(java.util.Comparator.comparing(Stock::getTicker)).toList();
        for (Stock stock : stocks) {
            try {
                refreshStock(stock);
                updated++;
            } catch (Exception exception) {
                failed++;
                log.warn("Could not refresh market data for {} ({})", stock.getTicker(), exception.getClass().getSimpleName());
            }
            // The free provider tier allows five requests per minute.
            if (updated + failed < stocks.size()) {
                try { TimeUnit.SECONDS.sleep(12); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); break; }
            }
        }
        Instant refreshedAt = Instant.now();
        if (updated > 0) {
            lastSuccessfulRefresh = refreshedAt;
            snapshotService.captureAllDaily();
        }
        return new RefreshResult("Massive EOD", updated, failed, refreshedAt);
    }

    protected void refreshStock(Stock stock) {
        // Exclude today's still-forming candle if someone refreshes during market hours.
        LocalDate to = LocalDate.now(EXCHANGE_ZONE).minusDays(1);
        LocalDate from = to.minusDays(365);
        JsonNode response = restClientBuilder.build().get()
                .uri(baseUrl + "/v2/aggs/ticker/{ticker}/range/1/day/{from}/{to}?adjusted=true&sort=asc&limit=50000&apiKey={key}",
                        stock.getTicker(), from, to, apiKey)
                .retrieve().body(JsonNode.class);
        JsonNode bars = response == null ? null : response.path("results");
        if (bars == null || !bars.isArray() || bars.isEmpty()) {
            throw new IllegalStateException("Provider returned no daily price history");
        }
        for (JsonNode bar : bars) {
            if (!bar.hasNonNull("t") || !bar.hasNonNull("c")) continue;
            LocalDate date = Instant.ofEpochMilli(bar.path("t").asLong()).atZone(EXCHANGE_ZONE).toLocalDate();
            BigDecimal close = BigDecimal.valueOf(bar.path("c").asDouble()).setScale(4, RoundingMode.HALF_UP);
            StockDailyPrice point = priceRepository.findByStockIdAndTradingDate(stock.getId(), date)
                    .orElseGet(StockDailyPrice::new);
            point.setStock(stock);
            point.setTradingDate(date);
            point.setClosePrice(close);
            point.setDataSource("MASSIVE_EOD");
            priceRepository.save(point);
            if (!date.isBefore(from)) {
                stock.setCurrentPrice(close);
                stock.setPriceUpdatedAt(Instant.now());
                stock.setPriceSource("MASSIVE_EOD");
            }
        }
        stockRepository.save(stock);
    }
}
