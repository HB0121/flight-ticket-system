package com.example.flight.crawl;

import java.time.LocalDate;

public interface FlightSyncPort {
    FlightSyncResult syncAirportDate(String airportCode, LocalDate date);

    static FlightSyncPort noop() {
        return (airportCode, date) -> FlightSyncResult.skipped(null);
    }
}
