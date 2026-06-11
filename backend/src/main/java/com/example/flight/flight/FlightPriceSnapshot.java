package com.example.flight.flight;

import java.time.LocalDateTime;

public record FlightPriceSnapshot(
        Long id,
        Long flightId,
        String flightNo,
        String fromCity,
        String toCity,
        LocalDateTime departTime,
        Integer price,
        Integer seatsLeft,
        String dataSource,
        LocalDateTime observedAt
) {
}
