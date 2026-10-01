package com.portfoliopro.backend.service;

import com.portfoliopro.backend.dto.TradeRequest;
import com.portfoliopro.backend.entity.*;
import com.portfoliopro.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final PortfolioSnapshotService portfolioSnapshotService;

    /**
     * Executes a buy or sell order at the stock's current price.
     * Validates funds (buy) or share quantity (sell), updates cash balance,
     * upserts the holding, and records the trade.
     */
    @Transactional
    public Trade executeTrade(TradeRequest request, String username) {
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String ticker = request.getTicker().trim().toUpperCase(java.util.Locale.ROOT);
        if (request.getIdempotencyKey() != null) {
            var previous = tradeRepository.findByUserIdAndIdempotencyKey(user.getId(), request.getIdempotencyKey());
            if (previous.isPresent()) {
                Trade trade = previous.get();
                if (trade.getStock().getTicker().equals(ticker)
                        && trade.getType() == request.getType()
                        && trade.getQuantity().equals(request.getQuantity())) return trade;
                throw new IllegalStateException("This idempotency key was already used for a different trade.");
            }
        }

        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.getTicker()));

        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no portfolio: " + user.getId()));

        BigDecimal price = stock.getCurrentPrice();
        if (price == null || price.signum() <= 0) {
            throw new IllegalStateException("This stock does not have a valid execution price.");
        }
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal realizedGainLoss = BigDecimal.ZERO;

        if (request.getType() == Trade.TradeType.BUY) {
            executeBuy(user, portfolio, stock, request.getQuantity(), price, totalCost);
        } else {
            realizedGainLoss = executeSell(user, portfolio, stock, request.getQuantity(), price, totalCost);
        }

        Trade trade = new Trade();
        trade.setUser(user);
        trade.setStock(stock);
        trade.setType(request.getType());
        trade.setQuantity(request.getQuantity());
        trade.setPriceAtExecution(price);
        trade.setRealizedGainLoss(realizedGainLoss);
        trade.setIdempotencyKey(request.getIdempotencyKey());
        Trade savedTrade = tradeRepository.save(trade);
        portfolioSnapshotService.capture(user.getUsername());
        return savedTrade;
    }

    private void executeBuy(User user, Portfolio portfolio, Stock stock, int quantity,
                             BigDecimal price, BigDecimal totalCost) {
        if (user.getCashBalance().compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient cash balance for this purchase");
        }

        user.setCashBalance(user.getCashBalance().subtract(totalCost));
        userRepository.save(user);

        Holding holding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId())
                .orElse(null);

        if (holding == null) {
            holding = new Holding();
            holding.setPortfolio(portfolio);
            holding.setStock(stock);
            holding.setQuantity(quantity);
            holding.setAverageCostBasis(price);
        } else {
            // Weighted average cost basis
            BigDecimal existingTotalCost = holding.getAverageCostBasis()
                    .multiply(BigDecimal.valueOf(holding.getQuantity()));
            BigDecimal newTotalCost = existingTotalCost.add(totalCost);
            int newQuantity;
            try {
                newQuantity = Math.addExact(holding.getQuantity(), quantity);
            } catch (ArithmeticException exception) {
                throw new IllegalStateException("The resulting share quantity is too large.");
            }

            holding.setQuantity(newQuantity);
            holding.setAverageCostBasis(newTotalCost.divide(BigDecimal.valueOf(newQuantity), 4, java.math.RoundingMode.HALF_UP));
        }

        holdingRepository.save(holding);
    }

    private BigDecimal executeSell(User user, Portfolio portfolio, Stock stock, int quantity,
                                   BigDecimal price, BigDecimal totalProceeds) {
        Holding holding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId())
                .orElseThrow(() -> new IllegalStateException("No existing position in " + stock.getTicker()));

        if (holding.getQuantity() < quantity) {
            throw new IllegalStateException("Cannot sell more shares than currently held");
        }

        BigDecimal realizedGainLoss = price.subtract(holding.getAverageCostBasis())
                .multiply(BigDecimal.valueOf(quantity));

        user.setCashBalance(user.getCashBalance().add(totalProceeds));
        userRepository.save(user);

        int remaining = holding.getQuantity() - quantity;
        if (remaining == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remaining);
            // average cost basis is unchanged on a partial sell
            holdingRepository.save(holding);
        }
        return realizedGainLoss;
    }
}
