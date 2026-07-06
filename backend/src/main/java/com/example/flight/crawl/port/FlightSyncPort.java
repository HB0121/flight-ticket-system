package com.example.flight.crawl.port;

import com.example.flight.crawl.model.FlightSyncResult;
import java.time.LocalDate;

public interface FlightSyncPort {
    FlightSyncResult syncAirportDate(String airportCode, LocalDate date);

    static FlightSyncPort noop() {
        return (airportCode, date) -> FlightSyncResult.skipped(null);
    }
}
