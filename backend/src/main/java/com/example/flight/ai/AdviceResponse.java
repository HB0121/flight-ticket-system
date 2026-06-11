package com.example.flight.ai;

import com.example.flight.flight.Flight;

import java.util.List;

public record AdviceResponse(
        String summary,
        Flight recommendedFlight,
        List<Flight> candidates
) {
}

