package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.User;

import java.math.BigDecimal;

public record UserProfileResponse(String username, String email, BigDecimal cashBalance) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getUsername(), user.getEmail(), user.getCashBalance());
    }
}
