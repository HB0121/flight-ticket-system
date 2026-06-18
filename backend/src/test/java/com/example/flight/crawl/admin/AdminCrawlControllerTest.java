package com.example.flight.crawl.admin;

import com.example.flight.config.GlobalExceptionHandler;
import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRepository;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCrawlControllerTest {

    private CrawlService crawlService;
    private CrawlRepository crawlRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        crawlService = mock(CrawlService.class);
        crawlRepository = mock(CrawlRepository.class);
        AdminCrawlController controller = new AdminCrawlController(crawlService, crawlRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsAdminCrawlJob() throws Exception {
        CrawlJob created = new CrawlJob(
                9L,
                "SUCCESS",
                LocalDateTime.parse("2026-06-18T10:00:00"),
                LocalDateTime.parse("2026-06-18T10:02:00"),
                12,
                0,
                null,
                "sample",
                "source=sample"
        );
        when(crawlService.runCrawler(any(CrawlRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/crawl-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"sample\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.source").value("sample"));

        verify(crawlService).runCrawler(any(CrawlRequest.class));
    }

    @Test
    void rejectsCreateJobWhenSourceIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/crawl-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(crawlService, never()).runCrawler(any(CrawlRequest.class));
    }

    @Test
    void rejectsCreateJobWhenSourceIsOutsidePhaseOneAllowlist() throws Exception {
        mockMvc.perform(post("/api/admin/crawl-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"ctrip_live\"}"))
                .andExpect(status().isBadRequest());

        verify(crawlService, never()).runCrawler(any(CrawlRequest.class));
    }

    @Test
    void listsRecentAdminCrawlJobs() throws Exception {
        when(crawlRepository.findRecent(20)).thenReturn(List.of(
                new CrawlJob(
                        8L,
                        "SUCCESS",
                        LocalDateTime.parse("2026-06-18T09:00:00"),
                        LocalDateTime.parse("2026-06-18T09:01:00"),
                        10,
                        0,
                        null,
                        "sample",
                        "source=sample"
                )
        ));

        mockMvc.perform(get("/api/admin/crawl-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }
}
