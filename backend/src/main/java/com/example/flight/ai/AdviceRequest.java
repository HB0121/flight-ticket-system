package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdviceRequest(
        @Size(max = 500, message = "出行需求描述不能超过500字")
        String message,
        @Size(max = 500, message = "出行需求描述不能超过500字")
        String query
) {
    public AdviceRequest(String message) {
        this(message, null);
    }

    @NotBlank(message = "出行需求描述不能为空")
    public String effectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        return query == null ? "" : query.trim();
    }
}
