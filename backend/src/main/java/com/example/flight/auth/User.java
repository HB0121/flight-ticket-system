package com.example.flight.auth;

import java.time.LocalDateTime;

/**
 * 用户实体（不可变记录类）。
 * <p>
 * 对应数据库表 {@code app_user}，存储用户的身份信息。
 * 密码字段存储的是 BCrypt 哈希值，而非明文。
 * </p>
 *
 * @param id        用户主键 ID（数据库自增）
 * @param username  登录用户名，全局唯一
 * @param password  BCrypt 加密后的密码哈希
 * @param nickname  显示昵称，默认与用户名相同
 * @param createdAt 注册时间
 */
public record User(
        Long id,
        String username,
        String password,
        String nickname,
        LocalDateTime createdAt
) {
}
