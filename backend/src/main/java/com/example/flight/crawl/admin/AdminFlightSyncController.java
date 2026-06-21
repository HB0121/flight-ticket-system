package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.FlightSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/flights")
public class AdminFlightSyncController {

    private final FlightSyncService flightSyncService;

    public AdminFlightSyncController(FlightSyncService flightSyncService) {
        this.flightSyncService = flightSyncService;
    }

    @PostMapping("/sync")
    public CrawlJob sync(@RequestParam String airportCode,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return flightSyncService.runAirportSyncJob(airportCode, date);
    }
}
