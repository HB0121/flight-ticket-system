package com.example.flight.crawl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class CrawlRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CrawlJob> rowMapper = (rs, rowNum) -> new CrawlJob(
            rs.getLong("id"),
            rs.getString("status"),
            rs.getTimestamp("started_at").toLocalDateTime(),
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toLocalDateTime(),
            rs.getInt("success_count"),
            rs.getInt("failed_count"),
            rs.getString("error_message")
    );

    public CrawlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CrawlJob> findLatest() {
        var jobs = jdbcTemplate.query("select * from crawl_job order by id desc limit 1", rowMapper);
        return jobs.stream().findFirst();
    }

    public void insertFailure(String message) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into crawl_job(status, started_at, finished_at, success_count, failed_count, error_message)
                values (?, ?, ?, ?, ?, ?)
                """, "FAILED", Timestamp.valueOf(now), Timestamp.valueOf(now), 0, 1, message);
    }
}

