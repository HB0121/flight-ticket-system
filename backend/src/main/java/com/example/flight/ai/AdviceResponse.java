package com.example.flight.ai;

import com.example.flight.flight.Flight;

import java.util.List;

public record AdviceResponse(
        String summary,
        AdviceIntentView intent,
        Flight recommendedFlight,
        List<Flight> candidates,
        boolean autoSynced,
        boolean syncAttempted,
        String syncStatus,
        String syncMessage,
        boolean fallbackUsed
) {
    public AdviceResponse(String summary,
                          AdviceIntentView intent,
                          Flight recommendedFlight,
                          List<Flight> candidates) {
        this(summary, intent, recommendedFlight, candidates, false, false, "SKIPPED", null, recommendedFlight == null);
    }
}
