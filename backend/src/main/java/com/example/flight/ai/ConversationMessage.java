package com.example.flight.ai;

import java.time.LocalDateTime;

/**
 * 对话消息实体 —— 表示对话会话中的一条消息。
 *
 * 每条消息属于一个会话，有角色（user / assistant）和内容文本。
 * 消息按创建时间正序排列，构成完整对话历史。
 *
 * 使用 Java record 实现不可变数据对象。
 *
 * @param id        消息唯一标识（自增主键，由数据库生成）
 * @param sessionId 所属会话 ID（关联 conversation_session.id）
 * @param role      消息角色："user"（用户消息）或 "assistant"（AI 回复）
 * @param content   消息文本内容
 * @param createdAt 消息创建时间
 */
public record ConversationMessage(
        Long id,
        String sessionId,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
