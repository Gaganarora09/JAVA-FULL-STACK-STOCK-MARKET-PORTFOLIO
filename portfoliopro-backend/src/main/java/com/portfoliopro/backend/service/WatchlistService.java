package com.portfoliopro.backend.service;

import com.portfoliopro.backend.dto.WatchlistItemResponse;
import com.portfoliopro.backend.entity.WatchlistEntry;
import com.portfoliopro.backend.repository.StockRepository;
import com.portfoliopro.backend.repository.UserRepository;
import com.portfoliopro.backend.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final WatchlistRepository watchlistRepository;

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> list(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return watchlistRepository.findByUserIdOrderByAddedAtDesc(user.getId()).stream()
                .map(WatchlistItemResponse::from).toList();
    }

    @Transactional
    public WatchlistItemResponse add(String username, String ticker) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        var stock = stockRepository.findByTicker(ticker.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker));
        var entry = watchlistRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseGet(() -> {
                    var newEntry = new WatchlistEntry();
                    newEntry.setUser(user);
                    newEntry.setStock(stock);
                    return watchlistRepository.save(newEntry);
                });
        return WatchlistItemResponse.from(entry);
    }

    @Transactional
    public void remove(String username, String ticker) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        var stock = stockRepository.findByTicker(ticker.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker));
        watchlistRepository.deleteByUserIdAndStockId(user.getId(), stock.getId());
    }
}
