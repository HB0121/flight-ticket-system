package com.example.flight.flight;

import java.time.LocalDate;

public record FlightSearchCriteria(
        String fromCity,
        String toCity,
        LocalDate date,
        String dataSource
) {
    public FlightSearchCriteria(String fromCity, String toCity, LocalDate date) {
        this(fromCity, toCity, date, null);
    }
}
