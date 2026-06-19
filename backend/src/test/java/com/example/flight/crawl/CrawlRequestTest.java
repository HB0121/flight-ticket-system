package com.example.flight.crawl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlRequestTest {

    @Test
    void normalizesLegacyAmadeusSourceToAerodataboxForAirportSync() {
        var request = new CrawlRequest("amadeus", null, null, LocalDate.parse("2026-06-19"), null, null, "ckg");

        assertThat(request.normalizedSource()).isEqualTo("aerodatabox");
        assertThat(request.toCrawlerArguments()).containsExactly(
                "scrapy", "crawl", "aerodatabox_flights",
                "-a", "source=aerodatabox",
                "-a", "airport_code=CKG",
                "-a", "date=2026-06-19"
        );
        assertThat(request.toSummary()).isEqualTo("source=aerodatabox, airportCode=CKG, date=2026-06-19");
    }

    @Test
    void requiresAirportCodeForAerodataboxRequests() {
        var request = new CrawlRequest("aerodatabox", null, null, LocalDate.parse("2026-06-19"), null, null, null);

        assertThatThrownBy(request::toCrawlerArguments)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("airportCode is required");
    }

    @Test
    void rejectsBlankSourceInsteadOfFallingBackToSampleCrawler() {
        var request = new CrawlRequest(null, null, null, null, null, null, null);

        assertThatThrownBy(request::toCrawlerArguments)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported source");
    }

    @Test
    void buildsLiveCrawlerArgumentsFromCtripRequest() {
        var request = new CrawlRequest("ctrip_live", "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), 1, 5, null);

        assertThat(request.toCrawlerArguments()).containsExactly(
                "scrapy", "crawl", "live_flights",
                "-a", "provider=ctrip",
                "-a", "from_city=Shanghai",
                "-a", "to_city=Beijing",
                "-a", "date=2026-06-19",
                "-a", "adults=1",
                "-a", "max_results=5"
        );
        assertThat(request.normalizedSource()).isEqualTo("ctrip_live");
        assertThat(request.toSummary()).isEqualTo("source=ctrip_live, fromCity=Shanghai, toCity=Beijing, date=2026-06-19, adults=1, maxResults=5");
    }

    @Test
    void buildsLiveCrawlerArgumentsFromFliggyAndQunarRequests() {
        assertThat(new CrawlRequest("fliggy_live", "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), 1, 5, null)
                .toCrawlerArguments())
                .contains("provider=fliggy");
        assertThat(new CrawlRequest("qunar_live", "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), 1, 5, null)
                .toCrawlerArguments())
                .contains("provider=qunar");
    }

    @Test
    void rejectsUnsupportedSourceInsteadOfFallingBackToSampleCrawler() {
        var request = new CrawlRequest("unknown_live", "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), 1, 5, null);

        assertThatThrownBy(request::toCrawlerArguments)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported source");
    }
}
