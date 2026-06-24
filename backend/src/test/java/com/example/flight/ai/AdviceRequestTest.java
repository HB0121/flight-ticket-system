package com.example.flight.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdviceRequestTest {

    @Test
    void usesMessageWhenPresent() {
        AdviceRequest request = new AdviceRequest("上海到北京", "重庆到北京");

        assertThat(request.effectiveMessage()).isEqualTo("上海到北京");
    }

    @Test
    void fallsBackToQueryWhenMessageIsBlank() {
        AdviceRequest request = new AdviceRequest("   ", "下周五去北京出差，预算1200元，希望上午到");

        assertThat(request.effectiveMessage()).isEqualTo("下周五去北京出差，预算1200元，希望上午到");
    }
}
