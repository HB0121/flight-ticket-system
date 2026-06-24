package com.example.flight.auth;

import java.time.LocalDateTime;

public record UserToken(
        Long id,
        Long userId,
        String token,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
