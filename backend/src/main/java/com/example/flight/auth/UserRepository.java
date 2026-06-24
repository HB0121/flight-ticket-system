package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("nickname"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        var users = jdbcTemplate.query(
                "select * from app_user where username = ?", userRowMapper, username);
        return users.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        var users = jdbcTemplate.query(
                "select * from app_user where id = ?", userRowMapper, id);
        return users.stream().findFirst();
    }

    public User insert(String username, String passwordHash, String nickname) {
        jdbcTemplate.update(
                "insert into app_user(username, password, nickname, created_at) values (?, ?, ?, ?)",
                username, passwordHash, nickname == null ? username : nickname,
                Timestamp.valueOf(LocalDateTime.now()));
        return findByUsername(username).orElseThrow();
    }

    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = ?", Long.class, username);
        return count != null && count > 0;
    }
}
