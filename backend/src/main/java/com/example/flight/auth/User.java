package com.example.flight.auth;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String password,
        String nickname,
        LocalDateTime createdAt
) {
}
