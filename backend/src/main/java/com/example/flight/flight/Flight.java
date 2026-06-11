package com.example.flight.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Flight(
        Long id,
        String flightNo,
        String airlineName,
        String fromCity,
        String toCity,
        String fromAirport,
        String toAirport,
        LocalDateTime departTime,
        LocalDateTime arriveTime,
        BigDecimal price,
        Integer seatsLeft,
        String dataSource,
        LocalDateTime collectedAt
) {
}

