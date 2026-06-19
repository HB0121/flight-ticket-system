package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/flights")
public class AdminFlightSyncController {

    private final CrawlService crawlService;
    private final DataSourceStatusService dataSourceStatusService;

    public AdminFlightSyncController(CrawlService crawlService,
                                     DataSourceStatusService dataSourceStatusService) {
        this.crawlService = crawlService;
        this.dataSourceStatusService = dataSourceStatusService;
    }

    @PostMapping("/sync")
    public CrawlJob sync(@RequestParam String airportCode,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (airportCode == null || airportCode.isBlank()) {
            throw new IllegalArgumentException("airportCode is required");
        }
        if (!dataSourceStatusService.isConfigured("aerodatabox")) {
            throw new IllegalStateException("source is not configured: aerodatabox");
        }
        return crawlService.runCrawler(new CrawlRequest(
                "aerodatabox",
                null,
                null,
                date,
                null,
                null,
                airportCode
        ));
    }
}
