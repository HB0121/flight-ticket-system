package com.example.flight.crawl.admin;

import java.time.LocalDate;

public record AdminCrawlJobRequest(
        String source,
        String fromCity,
        String toCity,
        LocalDate date,
        Integer adults,
        Integer maxResults,
        String airportCode
) {
}
