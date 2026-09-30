package com.portfoliopro.backend.service;

import com.portfoliopro.backend.dto.AuthResponse;
import com.portfoliopro.backend.dto.LoginRequest;
import com.portfoliopro.backend.dto.RegisterRequest;
import com.portfoliopro.backend.entity.Portfolio;
import com.portfoliopro.backend.entity.User;
import com.portfoliopro.backend.repository.PortfolioRepository;
import com.portfoliopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PortfolioSnapshotService portfolioSnapshotService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username) || userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCashBalance(new BigDecimal("100000.00"));
        user = userRepository.save(user);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolioRepository.save(portfolio);
        portfolioSnapshotService.capture(user.getUsername());
        return tokenResponse(user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return tokenResponse(user.getUsername());
    }

    private AuthResponse tokenResponse(String username) {
        return new AuthResponse(jwtService.generateToken(username), "Bearer", jwtService.getExpirationMs());
    }
}
