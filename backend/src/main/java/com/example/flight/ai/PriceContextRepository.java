package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PriceContextRepository {

    private static final Logger log = LoggerFactory.getLogger(PriceContextRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PriceContextRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveContext(String fromCity, String toCity, LocalDate departDate,
                            String contextText, String contextType) {
        jdbcTemplate.update(
                "insert into price_context(from_city, to_city, depart_date, context_text, context_type, created_at) values (?, ?, ?, ?, ?, ?)",
                fromCity, toCity, departDate, contextText, contextType,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    public List<String> searchContext(String fromCity, String toCity) {
        try {
            return jdbcTemplate.query(
                    "select context_text from price_context where (from_city = ? or ? is null or from_city = '') or (to_city = ? or ? is null or to_city = '') order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    fromCity, fromCity, toCity, toCity);
        } catch (Exception e) {
            log.warn("价格上下文检索失败，使用空结果: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    "select context_text from price_context where match(context_text) against (? in natural language mode) order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    keyword);
        } catch (Exception e) {
            log.debug("全文搜索不可用，使用LIKE降级: {}", e.getMessage());
            return jdbcTemplate.query(
                    "select context_text from price_context where context_text like ? order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    "%" + keyword + "%");
        }
    }

    public List<String> getRecentContext(String fromCity, String toCity, int limit) {
        return jdbcTemplate.query(
                "select context_text from price_context where from_city = ? and to_city = ? order by created_at desc limit ?",
                (rs, rowNum) -> rs.getString("context_text"),
                fromCity, toCity, limit);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from price_context", Long.class);
        return count == null ? 0 : count;
    }
}
