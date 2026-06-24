package com.example.flight.crawl;

public record FlightSyncResult(
        String status,
        String source,
        Integer successCount,
        Integer failedCount,
        String message
) {
    public static FlightSyncResult success(String source, Integer successCount, Integer failedCount, String message) {
        return new FlightSyncResult("SUCCESS", source, successCount, failedCount, message);
    }

    public static FlightSyncResult failed(String source, String message) {
        return new FlightSyncResult("FAILED", source, 0, 1, message);
    }

    public static FlightSyncResult skipped(String message) {
        return new FlightSyncResult("SKIPPED", "aerodatabox", 0, 0, message);
    }

    public static FlightSyncResult fromJob(CrawlJob job) {
        if (job == null) {
            return failed("aerodatabox", "同步未返回任务结果。");
        }
        String status = job.status() == null ? "UNKNOWN" : job.status();
        String source = job.source() == null ? "aerodatabox" : job.source();
        String message = job.errorMessage();
        if ("SUCCESS".equalsIgnoreCase(status) && (message == null || message.isBlank())) {
            message = "已自动同步该日期航班。";
        }
        return new FlightSyncResult(status.toUpperCase(), source, job.successCount(), job.failedCount(), message);
    }

    public boolean successful() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
