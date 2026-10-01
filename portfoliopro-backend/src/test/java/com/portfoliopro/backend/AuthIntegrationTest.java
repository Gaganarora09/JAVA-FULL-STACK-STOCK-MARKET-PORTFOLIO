package com.portfoliopro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfoliopro.backend.entity.User;
import com.portfoliopro.backend.repository.PortfolioRepository;
import com.portfoliopro.backend.repository.HoldingRepository;
import com.portfoliopro.backend.repository.TradeRepository;
import com.portfoliopro.backend.repository.WatchlistRepository;
import com.portfoliopro.backend.repository.UserRepository;
import com.portfoliopro.backend.repository.StockRepository;
import com.portfoliopro.backend.repository.PortfolioSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PortfolioRepository portfolioRepository;
    @Autowired HoldingRepository holdingRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired WatchlistRepository watchlistRepository;
    @Autowired StockRepository stockRepository;
    @Autowired PortfolioSnapshotRepository portfolioSnapshotRepository;

    @BeforeEach
    void cleanUsers() {
        watchlistRepository.deleteAll();
        tradeRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationHashesPasswordCreatesPortfolioAndTokenProtectsPortfolio() throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"correct-horse-1"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode auth = objectMapper.readTree(response);
        assertThat(auth.get("tokenType").asText()).isEqualTo("Bearer");
        User user = userRepository.findByUsername("alice").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("correct-horse-1");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(portfolioRepository.findByUserId(user.getId())).isPresent();

        mvc.perform(get("/api/portfolio/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/portfolio/me")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void loginRejectsWrongPasswordAndUserSpecificRoutesRequireAuthentication() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"bob@example.com","password":"correct-horse-1"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trades"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/stocks"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceType").value("SIMULATED_DEMO"));
    }

    @Test
    void tradesAreScopedToTokenOwnerAndResponsesDoNotExposeUserCredentials() throws Exception {
        String firstToken = registerAndGetToken("trader-one", "trader1@example.com");
        String secondToken = registerAndGetToken("trader-two", "trader2@example.com");

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"AAPL","type":"BUY","quantity":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(get("/api/trades").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(get("/api/trades").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("AAPL"));
        mvc.perform(get("/api/portfolio/me").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdings[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.totalUnrealizedGainLoss").value(0));
    }

    @Test
    void watchlistIsUserScopedAndAddingSameTickerTwiceDoesNotDuplicateIt() throws Exception {
        String firstToken = registerAndGetToken("watch-one", "watch1@example.com");
        String secondToken = registerAndGetToken("watch-two", "watch2@example.com");

        mvc.perform(post("/api/watchlist/aapl").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"));
        mvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/watchlist/AAPL").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
    }

    @Test
    void analyticsIncludesRealizedAndUnrealizedProfitAndAllocation() throws Exception {
        String token = registerAndGetToken("analyst", "analyst@example.com");
        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"AAPL","type":"BUY","quantity":2}
                                """))
                .andExpect(status().isOk());

        var stock = stockRepository.findByTicker("AAPL").orElseThrow();
        stock.setCurrentPrice(new java.math.BigDecimal("260.50"));
        stockRepository.save(stock);

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"AAPL","type":"SELL","quantity":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedGainLoss").value(15.00));

        mvc.perform(get("/api/portfolio/analytics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedGainLoss").value(15.00))
                .andExpect(jsonPath("$.unrealizedGainLoss").value(15.00))
                .andExpect(jsonPath("$.totalGainLoss").value(30.00))
                .andExpect(jsonPath("$.holdings[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.concentrationRiskEstimate").value("HIGH"))
                .andExpect(jsonPath("$.riskDescription").exists());
    }

    @Test
    void rejectedTradesDoNotChangeCashHoldingsOrTradeHistory() throws Exception {
        String token = registerAndGetToken("careful-trader", "careful@example.com");
        User user = userRepository.findByUsername("careful-trader").orElseThrow();

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"MSFT","type":"BUY","quantity":1000000}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"AAPL","type":"SELL","quantity":1}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"AAPL","type":"BUY","quantity":0}
                                """))
                .andExpect(status().isBadRequest());

        User unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getCashBalance()).isEqualByComparingTo("100000.00");
        assertThat(tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId())).isEmpty();
        Long portfolioId = portfolioRepository.findByUserId(user.getId()).orElseThrow().getId();
        assertThat(holdingRepository.countByPortfolioId(portfolioId)).isZero();
    }

    @Test
    void tradeExecutionUsesWeightedCostBasisAndIdempotencyKey() throws Exception {
        String token = registerAndGetToken("consistent-trader", "consistent@example.com");
        var stock = stockRepository.findByTicker("AAPL").orElseThrow();
        stock.setCurrentPrice(new java.math.BigDecimal("100.0000"));
        stockRepository.save(stock);

        String firstBuy = """
                {"ticker":"AAPL","type":"BUY","quantity":2,"idempotencyKey":"11111111-1111-4111-8111-111111111111"}
                """;
        String firstResponse = mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(firstBuy))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long originalTradeId = objectMapper.readTree(firstResponse).get("id").asLong();

        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(firstBuy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalTradeId));
        assertThat(tradeRepository.count()).isEqualTo(1);
        var userAfterReplay = userRepository.findByUsername("consistent-trader").orElseThrow();
        var portfolioAfterReplay = portfolioRepository.findByUserId(userAfterReplay.getId()).orElseThrow();
        var holdingAfterReplay = holdingRepository.findByPortfolioIdAndStockId(
                portfolioAfterReplay.getId(), stock.getId()).orElseThrow();
        assertThat(userAfterReplay.getCashBalance()).isEqualByComparingTo("99800.0000");
        assertThat(holdingAfterReplay.getQuantity()).isEqualTo(2);

        stock.setCurrentPrice(new java.math.BigDecimal("200.0000"));
        stockRepository.save(stock);
        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"type\":\"BUY\",\"quantity\":2}"))
                .andExpect(status().isOk());

        var user = userRepository.findByUsername("consistent-trader").orElseThrow();
        var portfolio = portfolioRepository.findByUserId(user.getId()).orElseThrow();
        var holding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId()).orElseThrow();
        assertThat(holding.getQuantity()).isEqualTo(4);
        assertThat(holding.getAverageCostBasis()).isEqualByComparingTo("150.0000");
        assertThat(user.getCashBalance()).isEqualByComparingTo("99400.0000");

        stock.setCurrentPrice(new java.math.BigDecimal("250.0000"));
        stockRepository.save(stock);
        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"type\":\"SELL\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedGainLoss").value(100.00));

        assertThat(holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId()).orElseThrow()
                .getQuantity()).isEqualTo(3);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getCashBalance())
                .isEqualByComparingTo("99650.0000");
        assertThat(tradeRepository.count()).isEqualTo(3);
        var snapshot = portfolioSnapshotRepository.findByUserIdAndValuationDate(
                user.getId(), java.time.LocalDate.now(java.time.ZoneOffset.UTC)).orElseThrow();
        assertThat(snapshot.getInvestedValue()).isEqualByComparingTo("750.0000");

        mvc.perform(get("/api/portfolio/performance?range=all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cashBalance").value(99650.00))
                .andExpect(jsonPath("$[0].investedValue").value(750.00))
                .andExpect(jsonPath("$[0].totalValue").value(100400.00))
                .andExpect(jsonPath("$[0].source").value("SIMULATED_DEMO_PRICES"));

        mvc.perform(get("/api/portfolio/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(99650.00))
                .andExpect(jsonPath("$.totalMarketValue").value(750.00))
                .andExpect(jsonPath("$.totalAccountValue").value(100400.00))
                .andExpect(jsonPath("$.totalUnrealizedGainLoss").value(300.00))
                .andExpect(jsonPath("$.holdings[0].quantity").value(3))
                .andExpect(jsonPath("$.holdings[0].averageCostBasis").value(150.0000));
    }

    @Test
    void idempotencyKeyCannotBeReusedForDifferentTrade() throws Exception {
        String token = registerAndGetToken("key-owner", "key-owner@example.com");
        String key = "22222222-2222-4222-8222-222222222222";
        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"type\":\"BUY\",\"quantity\":1,\"idempotencyKey\":\"" + key + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"type\":\"BUY\",\"quantity\":2,\"idempotencyKey\":\"" + key + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(tradeRepository.count()).isEqualTo(1);
    }

    private String registerAndGetToken(String username, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "email", email,
                                "password", "correct-horse-1"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
