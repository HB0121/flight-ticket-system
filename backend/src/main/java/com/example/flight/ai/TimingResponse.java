package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightPriceSnapshot;

import java.util.List;

public record TimingResponse(
        String summary,
        String riskLevel,
        String buyWindow,
        Flight recommendedFlight,
        List<FlightPriceSnapshot> history
) {
}
