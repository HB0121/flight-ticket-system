package com.example.flight.flight.history;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SearchHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SearchHistoryRecord> rowMapper = (rs, rowNum) -> new SearchHistoryRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("from_city"),
            rs.getString("to_city"),
            rs.getObject("travel_date", Date.class) == null ? null : rs.getObject("travel_date", Date.class).toLocalDate(),
            rs.getString("data_source"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public SearchHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchHistoryRecord append(Long userId, SearchHistoryCommand command) {
        jdbcTemplate.update(
                """
                insert into search_history(user_id, from_city, to_city, travel_date, data_source, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                userId,
                command.fromCity(),
                command.toCity(),
                command.travelDate(),
                command.dataSource(),
                Timestamp.valueOf(LocalDateTime.now()));
        return findByUserId(userId, 1).stream().findFirst().orElseThrow();
    }

    public List<SearchHistoryRecord> findByUserId(Long userId) {
        return findByUserId(userId, 20);
    }

    public List<SearchHistoryRecord> findByUserId(Long userId, int limit) {
        return jdbcTemplate.query(
                """
                select id, user_id, from_city, to_city, travel_date, data_source, created_at
                from search_history
                where user_id = ?
                order by created_at desc, id desc
                limit ?
                """,
                rowMapper,
                userId,
                limit);
    }
}
