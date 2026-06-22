package com.example.flight.auth;

public record MeResponse(
        Long id,
        String username,
        String nickname
) {
    static MeResponse from(User user) {
        return new MeResponse(user.id(), user.username(), user.nickname());
    }
}
