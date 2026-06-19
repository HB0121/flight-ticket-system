package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdviceIntentView(
        String from,
        String to,
        LocalDate date,
        BigDecimal budget,
        String timePreference
) {
    static AdviceIntentView from(ParsedTravelIntent intent) {
        if (intent == null) {
            return new AdviceIntentView(null, null, null, null, null);
        }
        return new AdviceIntentView(
                intent.fromCity(),
                intent.toCity(),
                intent.date(),
                intent.budget(),
                intent.timePreference() == null ? null : intent.timePreference().label()
        );
    }
}
