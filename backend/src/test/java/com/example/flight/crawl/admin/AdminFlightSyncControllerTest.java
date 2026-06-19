package com.example.flight.crawl.admin;

import com.example.flight.config.GlobalExceptionHandler;
import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminFlightSyncControllerTest {

    private CrawlService crawlService;
    private DataSourceStatusService dataSourceStatusService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        crawlService = mock(CrawlService.class);
        dataSourceStatusService = mock(DataSourceStatusService.class);
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(true);
        AdminFlightSyncController controller = new AdminFlightSyncController(crawlService, dataSourceStatusService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void triggersAirportSyncThroughCrawlerService() throws Exception {
        when(crawlService.runCrawler(any(CrawlRequest.class))).thenReturn(new CrawlJob(
                11L,
                "SUCCESS",
                LocalDateTime.parse("2026-06-18T10:00:00"),
                LocalDateTime.parse("2026-06-18T10:02:00"),
                0,
                0,
                null,
                "aerodatabox",
                "source=aerodatabox, airportCode=CKG, date=2026-06-18"
        ));

        mockMvc.perform(post("/api/admin/flights/sync")
                        .queryParam("airportCode", "CKG")
                        .queryParam("date", "2026-06-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("aerodatabox"));

        verify(crawlService).runCrawler(argThat(request ->
                "aerodatabox".equals(request.normalizedSource())
                        && "CKG".equalsIgnoreCase(request.airportCode())
        ));
    }

    @Test
    void rejectsSyncWhenSourceIsNotConfigured() throws Exception {
        when(dataSourceStatusService.isConfigured("aerodatabox")).thenReturn(false);

        mockMvc.perform(post("/api/admin/flights/sync")
                        .queryParam("airportCode", "CKG")
                        .queryParam("date", "2026-06-18"))
                .andExpect(status().isInternalServerError());

        verify(crawlService, never()).runCrawler(any(CrawlRequest.class));
    }
}
