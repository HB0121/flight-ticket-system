package com.example.flight.crawl;

import java.time.LocalDateTime;

public record CrawlJob(
        Long id,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Integer successCount,
        Integer failedCount,
        String errorMessage,
        String source,
        String actualSource,
        String fallbackReason,
        String requestParams
) {
    public CrawlJob(Long id,
                    String status,
                    LocalDateTime startedAt,
                    LocalDateTime finishedAt,
                    Integer successCount,
                    Integer failedCount,
                    String errorMessage) {
        this(id, status, startedAt, finishedAt, successCount, failedCount, errorMessage, null, null, null, null);
    }
}
