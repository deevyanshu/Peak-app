package com.peak.authenticationservice.Dto;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequestDto(@NotNull String refreshToken) {
}
