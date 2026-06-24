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
        String requestParams,
        Integer rejectedCount
) {
    public CrawlJob(Long id,
                    String status,
                    LocalDateTime startedAt,
                    LocalDateTime finishedAt,
                    Integer successCount,
                    Integer failedCount,
                    String errorMessage) {
        this(id, status, startedAt, finishedAt, successCount, failedCount, errorMessage, null, null, 0);
    }

    public CrawlJob(Long id,
                    String status,
                    LocalDateTime startedAt,
                    LocalDateTime finishedAt,
                    Integer successCount,
                    Integer failedCount,
                    String errorMessage,
                    String source,
                    String requestParams) {
        this(id, status, startedAt, finishedAt, successCount, failedCount, errorMessage, source, requestParams, 0);
    }
}
