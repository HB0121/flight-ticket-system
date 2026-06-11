package com.example.flight.crawl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlRequestTest {
    @Test
    void buildsAmadeusCrawlerArgumentsFromRequest() {
        var request = new CrawlRequest("amadeus", "上海", "北京", LocalDate.parse("2026-06-19"), 1, 3);

        assertThat(request.toCrawlerArguments()).containsExactly(
                "scrapy", "crawl", "amadeus_flights",
                "-a", "from_city=上海",
                "-a", "to_city=北京",
                "-a", "date=2026-06-19",
                "-a", "adults=1",
                "-a", "max_results=3"
        );
        assertThat(request.toSummary()).isEqualTo("source=amadeus, fromCity=上海, toCity=北京, date=2026-06-19, adults=1, maxResults=3");
    }

    @Test
    void defaultsToSampleCrawlerWhenSourceIsBlank() {
        var request = new CrawlRequest(null, null, null, null, null, null);

        assertThat(request.toCrawlerArguments()).containsExactly("scrapy", "crawl", "sample_flights");
        assertThat(request.normalizedSource()).isEqualTo("sample");
    }
}
