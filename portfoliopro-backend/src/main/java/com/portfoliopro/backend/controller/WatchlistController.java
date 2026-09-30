package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.WatchlistItemResponse;
import com.portfoliopro.backend.service.WatchlistService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Validated
public class WatchlistController {
    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItemResponse> list(Authentication authentication) {
        return watchlistService.list(authentication.getName());
    }

    @PostMapping("/{ticker}")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistItemResponse add(@PathVariable @NotBlank String ticker, Authentication authentication) {
        return watchlistService.add(authentication.getName(), ticker);
    }

    @DeleteMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable @NotBlank String ticker, Authentication authentication) {
        watchlistService.remove(authentication.getName(), ticker);
    }
}
