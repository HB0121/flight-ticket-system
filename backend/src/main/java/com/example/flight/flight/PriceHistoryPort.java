package com.example.flight.flight;

import java.util.List;

public interface PriceHistoryPort {
    List<FlightPriceSnapshot> findPriceHistory(Long flightId);
}
