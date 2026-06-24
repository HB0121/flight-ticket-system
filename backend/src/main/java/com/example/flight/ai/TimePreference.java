package com.example.flight.ai;

enum TimePreference {
    EARLY_MORNING("凌晨"),
    MORNING("上午"),
    AFTERNOON("下午"),
    EVENING("晚上");

    private final String label;

    TimePreference(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
