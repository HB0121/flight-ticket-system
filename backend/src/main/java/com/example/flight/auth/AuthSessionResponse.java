package com.example.flight.auth;

public record AuthSessionResponse(
        Long id,
        String username,
        String nickname,
        String token
) {
    static AuthSessionResponse from(AuthService.LoginResponse response) {
        return new AuthSessionResponse(response.id(), response.username(), response.nickname(), response.token());
    }
}
