package com.example.flight.auth;

import java.time.LocalDateTime;

/**
 * 用户登录令牌实体（不可变记录类）。
 * <p>
 * 对应数据库表 {@code user_token}，存储用户登录会话。
 * 每次登录或注册时生成一个新的 UUID token，有效期为 7 天。
 * 客户端通过 HTTP 请求头 {@code Authorization: Bearer <token>} 携带此令牌。
 * </p>
 *
 * @param id        令牌主键 ID（数据库自增）
 * @param userId    关联的用户 ID，外键指向 {@code app_user.id}
 * @param token     令牌字符串，由 {@link java.util.UUID#randomUUID()} 生成
 * @param createdAt 令牌创建时间
 * @param expiresAt 令牌过期时间（创建时间 + 7 天）
 */
public record UserToken(
        Long id,
        Long userId,
        String token,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
