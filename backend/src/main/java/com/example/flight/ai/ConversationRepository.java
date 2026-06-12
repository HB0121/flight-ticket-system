package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private static final Logger log = LoggerFactory.getLogger(ConversationRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ConversationSession> sessionRowMapper = (rs, rowNum) -> new ConversationSession(
            rs.getString("id"),
            rs.getString("title"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<ConversationMessage> messageRowMapper = (rs, rowNum) -> new ConversationMessage(
            rs.getLong("id"),
            rs.getString("session_id"),
            rs.getString("role"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ConversationSession createSession(String title) {
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                "insert into conversation_session(id, title, created_at, updated_at) values (?, ?, ?, ?)",
                id, title, now, now);
        return new ConversationSession(id, title, now.toLocalDateTime(), now.toLocalDateTime());
    }

    public void addMessage(String sessionId, String role, String content) {
        jdbcTemplate.update(
                "insert into conversation_message(session_id, role, content, created_at) values (?, ?, ?, ?)",
                sessionId, role, content, Timestamp.valueOf(LocalDateTime.now()));
        jdbcTemplate.update(
                "update conversation_session set updated_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now()), sessionId);
    }

    public List<ConversationMessage> getMessages(String sessionId) {
        return jdbcTemplate.query(
                "select * from conversation_message where session_id = ? order by created_at asc",
                messageRowMapper, sessionId);
    }

    public List<ConversationSession> listSessions(int limit) {
        return jdbcTemplate.query(
                "select * from conversation_session order by updated_at desc limit ?",
                sessionRowMapper, limit);
    }

    public void deleteSession(String sessionId) {
        jdbcTemplate.update("delete from conversation_message where session_id = ?", sessionId);
        jdbcTemplate.update("delete from conversation_session where id = ?", sessionId);
    }
}
