package com.example.flight.ai;

import java.util.Optional;

@FunctionalInterface
public interface AiTextClient {
    Optional<String> generate(String systemPrompt, String userPrompt);
}
