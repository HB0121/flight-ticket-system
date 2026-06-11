package com.example.flight.flight;

import java.time.LocalDate;

public record FlightSearchCriteria(
        String fromCity,
        String toCity,
        LocalDate date
) {
}

