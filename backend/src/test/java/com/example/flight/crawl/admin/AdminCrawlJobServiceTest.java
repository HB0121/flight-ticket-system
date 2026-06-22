package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRepository;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCrawlJobServiceTest {

    @Test
    void createsAdminCrawlJobWithLegacySourceAlias() {
        CrawlService crawlService = mock(CrawlService.class);
        CrawlRepository crawlRepository = mock(CrawlRepository.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        AdminCrawlJobService service = new AdminCrawlJobService(crawlService, crawlRepository, dataSourceStatusService);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(true);
        CrawlJob created = crawlJob(9L);
        when(crawlService.runCrawler(any(CrawlRequest.class))).thenReturn(created);

        CrawlJob result = service.create(new AdminCrawlJobRequest(
                "amadeus", null, null, LocalDate.parse("2026-06-18"), null, null, "CKG"));

        assertThat(result).isSameAs(created);
        verify(crawlService).runCrawler(argThat(request ->
                "aerodatabox".equals(request.normalizedSource())
                        && "CKG".equalsIgnoreCase(request.airportCode())
        ));
    }

    @Test
    void rejectsCreateJobWhenSourceIsOutsideAllowlist() {
        CrawlService crawlService = mock(CrawlService.class);
        CrawlRepository crawlRepository = mock(CrawlRepository.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        AdminCrawlJobService service = new AdminCrawlJobService(crawlService, crawlRepository, dataSourceStatusService);

        assertThatThrownBy(() -> service.create(new AdminCrawlJobRequest(
                "ctrip_live", null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("source must be one of: aerodatabox, amadeus");

        verify(crawlService, never()).runCrawler(any(CrawlRequest.class));
    }

    @Test
    void rejectsCreateJobWhenSourceIsNotConfigured() {
        CrawlService crawlService = mock(CrawlService.class);
        CrawlRepository crawlRepository = mock(CrawlRepository.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        AdminCrawlJobService service = new AdminCrawlJobService(crawlService, crawlRepository, dataSourceStatusService);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(false);

        assertThatThrownBy(() -> service.create(new AdminCrawlJobRequest(
                "aerodatabox", null, null, LocalDate.parse("2026-06-18"), null, null, "CKG")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("source is not configured: aerodatabox");

        verify(crawlService, never()).runCrawler(any(CrawlRequest.class));
    }

    @Test
    void listsRecentAdminCrawlJobs() {
        CrawlService crawlService = mock(CrawlService.class);
        CrawlRepository crawlRepository = mock(CrawlRepository.class);
        DataSourceStatusService dataSourceStatusService = mock(DataSourceStatusService.class);
        AdminCrawlJobService service = new AdminCrawlJobService(crawlService, crawlRepository, dataSourceStatusService);
        when(crawlRepository.findRecent(20)).thenReturn(List.of(crawlJob(8L)));

        assertThat(service.list()).extracting(CrawlJob::id).containsExactly(8L);
    }

    private static CrawlJob crawlJob(Long id) {
        return new CrawlJob(
                id,
                "SUCCESS",
                LocalDateTime.parse("2026-06-18T10:00:00"),
                LocalDateTime.parse("2026-06-18T10:02:00"),
                12,
                0,
                null,
                "aerodatabox",
                "source=aerodatabox, airportCode=CKG, date=2026-06-18"
        );
    }
}
