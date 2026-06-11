package com.example.flight.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DeepSeekTextClient implements AiTextClient {
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public DeepSeekTextClient(@Value("${app.deepseek.api-key:}") String apiKey,
                              @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                              @Value("${app.deepseek.model:deepseek-v4-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "stream", false
            );
            Map<?, ?> response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return extractContent(response);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> extractContent(Map<?, ?> response) {
        if (response == null) {
            return Optional.empty();
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return Optional.empty();
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return Optional.empty();
        }
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            return Optional.empty();
        }
        Object content = message.get("content");
        return content == null || !StringUtils.hasText(content.toString())
                ? Optional.empty()
                : Optional.of(content.toString().trim());
    }
}
