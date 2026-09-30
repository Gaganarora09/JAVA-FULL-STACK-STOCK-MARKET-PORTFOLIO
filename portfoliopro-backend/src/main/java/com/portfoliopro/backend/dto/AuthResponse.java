package com.portfoliopro.backend.dto;

public record AuthResponse(String token, String tokenType, long expiresIn) {}
