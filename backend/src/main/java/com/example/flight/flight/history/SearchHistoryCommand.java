package com.example.flight.flight.history;

import java.time.LocalDate;

public record SearchHistoryCommand(
        String fromCity,
        String toCity,
        LocalDate travelDate,
        String dataSource
) {
}
