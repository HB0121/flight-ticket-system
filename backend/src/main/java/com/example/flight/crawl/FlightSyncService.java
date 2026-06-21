package com.example.flight.crawl;

import com.example.flight.crawl.admin.DataSourceStatusService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FlightSyncService implements FlightSyncPort {
    private static final String SOURCE = "aerodatabox";

    private final CrawlService crawlService;
    private final DataSourceStatusService dataSourceStatusService;

    public FlightSyncService(CrawlService crawlService,
                             DataSourceStatusService dataSourceStatusService) {
        this.crawlService = crawlService;
        this.dataSourceStatusService = dataSourceStatusService;
    }

    public CrawlJob runAirportSyncJob(String airportCode, LocalDate date) {
        if (airportCode == null || airportCode.isBlank()) {
            throw new IllegalArgumentException("airportCode is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (!dataSourceStatusService.isConfigured(SOURCE)) {
            throw new IllegalStateException("source is not configured: " + SOURCE);
        }
        return crawlService.runCrawler(new CrawlRequest(
                SOURCE,
                null,
                null,
                date,
                null,
                null,
                airportCode
        ));
    }

    @Override
    public FlightSyncResult syncAirportDate(String airportCode, LocalDate date) {
        try {
            return FlightSyncResult.fromJob(runAirportSyncJob(airportCode, date));
        } catch (RuntimeException ex) {
            return FlightSyncResult.failed(SOURCE, ex.getMessage());
        }
    }
}
