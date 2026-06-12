package com.example.flight.ai;

import java.time.LocalDateTime;

public record ConversationSession(
        String id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
