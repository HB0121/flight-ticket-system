package com.example.flight.crawl;

import java.time.LocalDateTime;

public record CrawlJob(
        Long id,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Integer successCount,
        Integer failedCount,
        String errorMessage
) {
}

