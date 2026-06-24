package com.example.flight.ai;

import java.time.LocalDateTime;

public record ConversationMessage(
        Long id,
        String sessionId,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
