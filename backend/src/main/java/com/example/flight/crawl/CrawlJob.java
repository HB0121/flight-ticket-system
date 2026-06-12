package com.example.flight.crawl;

import java.time.LocalDateTime;

/**
 * 爬虫任务实体，对应数据库 crawl_job 表的一行记录。
 * 每次触发爬虫（手动或 API）都会创建一条 CrawlJob 记录，
 * 用于追踪爬虫的执行状态和统计数据。
 *
 * 使用 Java Record 提供不可变性，并通过多个紧凑构造器实现向后兼容：
 * 新字段（source, requestParams, rejectedCount）有默认值，
 * 老代码无需修改即可编译通过。
 *
 * @param id            任务主键（自增）
 * @param status        执行状态（RUNNING / SUCCESS / FAILED / UNKNOWN）
 * @param startedAt     开始时间
 * @param finishedAt    结束时间（失败时与 startedAt 相近）
 * @param successCount  成功采集的航班数量
 * @param failedCount   采集失败的数量
 * @param errorMessage  错误信息（成功时为空）
 * @param source        数据来源（"amadeus" / "sample"）
 * @param requestParams 请求参数摘要（如 "fromCity=上海, toCity=北京"）
 * @param rejectedCount 因校验不通过被拒绝的记录数
 */
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
    /**
     * v1 兼容构造器：source 和 requestParams 默认为 null，rejectedCount 默认为 0。
     * 用于不需要数据来源和参数记录的场景（如旧版 RowMapper）。
     */
    public CrawlJob(Long id,
                    String status,
                    LocalDateTime startedAt,
                    LocalDateTime finishedAt,
                    Integer successCount,
                    Integer failedCount,
                    String errorMessage) {
        this(id, status, startedAt, finishedAt, successCount, failedCount, errorMessage, null, null, 0);
    }

    /**
     * v2 兼容构造器：rejectedCount 默认为 0。
     * 用于尚未记录拒绝数量的场景。
     */
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
