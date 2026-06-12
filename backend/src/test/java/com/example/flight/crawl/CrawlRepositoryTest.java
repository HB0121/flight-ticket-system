package com.example.flight.crawl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(CrawlRepository.class)
class CrawlRepositoryTest {
    @Autowired
    private CrawlRepository crawlRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mapsActualSourceAndFallbackReasonFromLatestJob() {
        jdbcTemplate.update("""
                insert into crawl_job(
                    status, started_at, finished_at, success_count, failed_count,
                    error_message, source, actual_source, fallback_reason, request_params
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "SUCCESS", "2026-06-11 10:00:00", "2026-06-11 10:01:00", 4, 0,
                null, "amadeus", "sample", "Amadeus returned no rows; using sample fallback.",
                "source=amadeus, fromCity=上海, toCity=北京");

        var job = crawlRepository.findLatest().orElseThrow();

        assertThat(job.source()).isEqualTo("amadeus");
        assertThat(job.actualSource()).isEqualTo("sample");
        assertThat(job.fallbackReason()).contains("using sample fallback");
    }
}
