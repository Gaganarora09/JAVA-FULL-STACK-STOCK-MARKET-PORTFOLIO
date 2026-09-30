package com.portfoliopro.backend.controller;

import com.portfoliopro.backend.dto.UserProfileResponse;
import com.portfoliopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
