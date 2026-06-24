package com.example.flight.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TokenRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserToken> tokenRowMapper = (rs, rowNum) -> new UserToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("expires_at").toLocalDateTime()
    );

    public TokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserToken createToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(7);
        jdbcTemplate.update(
                "insert into user_token(user_id, token, created_at, expires_at) values (?, ?, ?, ?)",
                userId, token, Timestamp.valueOf(now), Timestamp.valueOf(expires));
        return findByToken(token).orElseThrow();
    }

    public Optional<UserToken> findByToken(String token) {
        var tokens = jdbcTemplate.query(
                "select * from user_token where token = ? and expires_at > ?",
                tokenRowMapper, token, Timestamp.valueOf(LocalDateTime.now()));
        return tokens.stream().findFirst();
    }

    public void deleteByToken(String token) {
        jdbcTemplate.update("delete from user_token where token = ?", token);
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("delete from user_token where user_id = ?", userId);
    }
}
