package com.example.flight.flight.repository;

import com.example.flight.flight.model.FlightPriceSnapshot;
import java.util.List;

public interface PriceHistoryPort {
    List<FlightPriceSnapshot> findPriceHistory(Long flightId);
}
