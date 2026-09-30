package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.UserProfileResponse;
import com.portfoliopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PostMapping("/me/demo-funds")
    @Transactional
    public UserProfileResponse addPracticeFunds(Authentication authentication) {
        var user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCashBalance(user.getCashBalance().add(new BigDecimal("100000.00")));
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }
}
