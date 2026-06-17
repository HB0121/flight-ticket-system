package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户数据访问仓库。
 * <p>
 * 使用 Spring JdbcTemplate 直接操作 {@code app_user} 表，
 * 提供用户查询、插入和存在性检查功能。
 * 采用 RowMapper 将数据库行映射为不可变的 {@link User} 记录。
 * </p>
 * <p>
 * 设计模式：Repository 模式 —— 封装数据访问逻辑，与业务逻辑解耦。
 * </p>
 */
@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    /** Spring JDBC 模板，由构造函数注入 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 数据库行到 User 记录的映射器。
     * 从 ResultSet 中提取各列并构建不可变的 User 对象。
     */
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    /**
     * 构造函数注入 JdbcTemplate。
     *
     * @param jdbcTemplate Spring 自动注入的 JDBC 模板
     */
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 包含用户的 Optional，若不存在则为 Optional.empty()
     */
    public Optional<User> findByUsername(String username) {
        var users = jdbcTemplate.query(
                "select * from app_user where username = ?", userRowMapper, username);
        return users.stream().findFirst();
    }

    /**
     * 根据主键 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 包含用户的 Optional，若不存在则为 Optional.empty()
     */
    public Optional<User> findById(Long id) {
        var users = jdbcTemplate.query(
                "select * from app_user where id = ?", userRowMapper, id);
        return users.stream().findFirst();
    }

    /**
     * 插入新用户。
     * <p>
     * 昵称默认为用户名（当 nickname 为 null 时）。
     * 创建时间使用当前系统时间。
     * </p>
     *
     * @param username     用户名
     * @param passwordHash BCrypt 加密后的密码哈希
     * @param nickname     显示昵称（可为 null，默认取用户名）
     * @return 插入后的完整用户对象（含自增 ID）
     */
    public User insert(String username, String passwordHash) {
        jdbcTemplate.update(
                "insert into app_user(username, password, created_at) values (?, ?, ?)",
                username, passwordHash,
                Timestamp.valueOf(LocalDateTime.now()));
        return findByUsername(username).orElseThrow();
    }

    /**
     * 检查用户名是否已被注册。
     *
     * @param username 待检查的用户名
     * @return true 表示用户名已存在，false 表示可用
     */
    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = ?", Long.class, username);
        return count != null && count > 0;
    }
}
