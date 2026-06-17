package com.example.flight.crawl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 爬虫任务数据访问仓库，封装 crawl_job 表的 CRUD 操作。
 * 与 FlightRepository 风格一致，使用 JDBC 模板 + RowMapper 模式。
 *
 * 职责：
 *   - 查询最近一次爬虫任务（用于前端状态展示）
 *   - 记录失败的爬虫任务（CrawlService 在异常时调用）
 *
 * RowMapper 设计中处理 finished_at 可能为 NULL 的情况：
 * 若 finished_at 在数据库中为 NULL（爬虫进行中），映射为 Java null，
 * 调用方可通过 null 判断爬虫是否仍在执行。
 */
@Repository
public class CrawlRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * CrawlJob 表的行映射器。
     * 注意：finished_at 可能为 NULL（爬虫正在执行中的记录），
     * 此时 getTimestamp 返回 null，映射结果仍为 null。
     */
    private final RowMapper<CrawlJob> rowMapper = (rs, rowNum) -> new CrawlJob(
            rs.getLong("id"),
            rs.getString("status"),
            rs.getTimestamp("started_at").toLocalDateTime(),
            // finished_at 可能为 NULL：返回 null（表示爬虫尚未结束）
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toLocalDateTime(),
            rs.getInt("success_count"),
            rs.getInt("failed_count"),
            rs.getString("error_message"),
            rs.getString("source"),
            rs.getString("request_params")
    );

    /** 构造器注入 JdbcTemplate */
    public CrawlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询最近一次爬虫任务记录（按 ID 倒序取第一条）。
     * 用于前端 /api/crawl/latest 接口和 CrawlService.execute 的返回值。
     *
     * @return Optional 包装的最新 CrawlJob，表中无记录时返回 Optional.empty()
     */
    public Optional<CrawlJob> findLatest() {
        var jobs = jdbcTemplate.query("select * from crawl_job order by id desc limit 1", rowMapper);
        return jobs.stream().findFirst();
    }

    /**
     * 插入一条失败状态的任务记录（不带 source 信息，兼容 v1 调用方）。
     * 标记为 FAILED 状态，失败计数为 1。
     *
     * @param message 错误描述信息
     */
    public void insertFailure(String message) {
        insertFailure("sample", "", message);
    }

    /**
     * 插入一条失败状态的任务记录（完整版本）。
     * 同时记录数据来源和请求参数，方便后续排查问题。
     *
     * SQL 模式：直接 INSERT（非 UPDATE），保留每次失败记录作为审计日志，
     * 不覆盖已有记录。started_at 和 finished_at 均设为当前时刻（失败瞬间完成）。
     *
     * @param source        数据来源标识
     * @param requestParams 请求参数摘要
     * @param message       错误描述信息
     */
    public void insertFailure(String source, String requestParams, String message) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "insert into crawl_job(status, started_at, finished_at, success_count, failed_count, error_message, source, request_params)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?)",
                "FAILED", Timestamp.valueOf(now), Timestamp.valueOf(now), 0, 1, message, source, requestParams);
    }
}
