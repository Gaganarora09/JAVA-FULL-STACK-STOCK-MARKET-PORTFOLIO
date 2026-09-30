package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.TradeRequest;
import com.portfoliopro.backend.dto.TradeResponse;
import com.portfoliopro.backend.entity.Trade;
import com.portfoliopro.backend.repository.TradeRepository;
import com.portfoliopro.backend.repository.UserRepository;
import com.portfoliopro.backend.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    @PostMapping
    public TradeResponse placeTrade(@Valid @RequestBody TradeRequest request, Authentication authentication) {
        return TradeResponse.from(tradeService.executeTrade(request, authentication.getName()));
    }

    @GetMapping
    public List<TradeResponse> getTradeHistory(Authentication authentication) {
        Long userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return tradeRepository.findByUserIdOrderByExecutedAtDesc(userId).stream()
                .map(TradeResponse::from).toList();
    }
}
