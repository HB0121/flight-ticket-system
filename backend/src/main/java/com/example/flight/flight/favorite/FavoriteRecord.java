package com.example.flight.flight.favorite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteRecord(
        Long id,
        Long userId,
        Long flightId,
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
        LocalDateTime collectedAt,
        LocalDateTime createdAt
) {
}
