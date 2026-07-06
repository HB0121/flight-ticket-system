package com.example.flight.crawl.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateCrawlJobRequest(
        @NotBlank String source,
        String fromCity,
        String toCity,
        LocalDate date,
        @Min(1) Integer adults,
        @Min(1) Integer maxResults,
        String airportCode
) {
}
