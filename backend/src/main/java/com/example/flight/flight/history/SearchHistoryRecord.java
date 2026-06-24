package com.example.flight.flight.history;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SearchHistoryRecord(
        Long id,
        Long userId,
        String fromCity,
        String toCity,
        LocalDate travelDate,
        String dataSource,
        LocalDateTime createdAt
) {
}
