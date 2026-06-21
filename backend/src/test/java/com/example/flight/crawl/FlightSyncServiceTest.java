package com.example.flight.crawl;

import com.example.flight.crawl.admin.DataSourceStatusService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightSyncServiceTest {
    @Test
    void runsAirportSyncThroughCrawlerService() {
        CrawlService crawlService = mock(CrawlService.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(true);
        when(crawlService.runCrawler(argThat(request ->
                "aerodatabox".equals(request.normalizedSource())
                        && "CKG".equalsIgnoreCase(request.airportCode())
                        && LocalDate.parse("2026-06-26").equals(request.date())
        ))).thenReturn(new CrawlJob(
                7L,
                "SUCCESS",
                LocalDateTime.parse("2026-06-26T10:00:00"),
                LocalDateTime.parse("2026-06-26T10:01:00"),
                12,
                0,
                null,
                "aerodatabox",
                "source=aerodatabox, airportCode=CKG, date=2026-06-26"
        ));
        FlightSyncService service = new FlightSyncService(crawlService, dataSourceStatusService);

        FlightSyncResult result = service.syncAirportDate("CKG", LocalDate.parse("2026-06-26"));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isEqualTo(12);
        verify(crawlService).runCrawler(argThat(request -> "CKG".equalsIgnoreCase(request.airportCode())));
    }

    @Test
    void controllerPathStillThrowsWhenSourceIsNotConfigured() {
        CrawlService crawlService = mock(CrawlService.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(false);
        FlightSyncService service = new FlightSyncService(crawlService, dataSourceStatusService);

        assertThatThrownBy(() -> service.runAirportSyncJob("CKG", LocalDate.parse("2026-06-26")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source is not configured");
    }

    @Test
    void autoSyncPathConvertsConfigurationFailureToFailedResult() {
        CrawlService crawlService = mock(CrawlService.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(false);
        FlightSyncService service = new FlightSyncService(crawlService, dataSourceStatusService);

        FlightSyncResult result = service.syncAirportDate("CKG", LocalDate.parse("2026-06-26"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.message()).contains("source is not configured");
    }
}
