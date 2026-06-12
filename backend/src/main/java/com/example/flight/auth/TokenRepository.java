package com.example.flight.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户令牌数据访问仓库。
 * <p>
 * 使用 Spring JdbcTemplate 操作 {@code user_token} 表，
 * 管理登录会话令牌的生命周期：创建、查询（含过期校验）、删除。
 * </p>
 * <p>
 * 令牌生成策略：使用 {@link UUID#randomUUID()} 生成全局唯一的随机字符串，
 * 有效期为创建时间 + 7 天。查询时同时校验 token 匹配和未过期两个条件。
 * </p>
 * <p>
 * 设计模式：Repository 模式 —— 封装令牌的持久化逻辑。
 * </p>
 */
@Repository
public class TokenRepository {

    /** Spring JDBC 模板，由构造函数注入 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 数据库行到 UserToken 记录的映射器。
     * 从 ResultSet 中提取各列并构建不可变的 UserToken 对象。
     */
    private final RowMapper<UserToken> tokenRowMapper = (rs, rowNum) -> new UserToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("expires_at").toLocalDateTime()
    );

    /**
     * 构造函数注入 JdbcTemplate。
     *
     * @param jdbcTemplate Spring 自动注入的 JDBC 模板
     */
    public TokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 为用户创建新的登录令牌。
     * <p>
     * 使用 UUID 生成随机令牌字符串，有效期设为 7 天。
     * 每次登录都会生成新令牌，旧令牌不会被自动删除（登出时清理）。
     * </p>
     *
     * @param userId 用户 ID
     * @return 新创建的令牌实体（含自增 ID、token 字符串、过期时间）
     */
    public UserToken createToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(7);
        jdbcTemplate.update(
                "insert into user_token(user_id, token, created_at, expires_at) values (?, ?, ?, ?)",
                userId, token, Timestamp.valueOf(now), Timestamp.valueOf(expires));
        return findByToken(token).orElseThrow();
    }

    /**
     * 根据 token 字符串查找有效的令牌。
     * <p>
     * 查询条件：token 完全匹配 AND 过期时间大于当前时间。
     * 已过期的令牌不会被返回，相当于自动失效。
     * </p>
     *
     * @param token 令牌字符串
     * @return 包含有效令牌的 Optional，若无效或已过期则为 Optional.empty()
     */
    public Optional<UserToken> findByToken(String token) {
        var tokens = jdbcTemplate.query(
                "select * from user_token where token = ? and expires_at > ?",
                tokenRowMapper, token, Timestamp.valueOf(LocalDateTime.now()));
        return tokens.stream().findFirst();
    }

    /**
     * 根据 token 字符串删除令牌（用于登出操作）。
     *
     * @param token 令牌字符串
     */
    public void deleteByToken(String token) {
        jdbcTemplate.update("delete from user_token where token = ?", token);
    }

    /**
     * 删除指定用户的所有令牌（用于强制下线场景）。
     *
     * @param userId 用户 ID
     */
    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("delete from user_token where user_id = ?", userId);
    }
}
