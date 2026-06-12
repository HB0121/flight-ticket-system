package com.example.flight.ai;

import java.time.LocalDateTime;

/**
 * 对话会话实体 —— 表示一次多轮对话会话。
 *
 * 每个会话包含一个唯一 ID、标题和创建/更新时间。
 * 会话与其消息（ConversationMessage）之间通过 session_id 建立一对多关联。
 *
 * 使用 Java record 实现不可变数据对象。
 *
 * @param id        会话唯一标识（UUID 字符串，由 ConversationRepository 生成）
 * @param title     会话标题（用户自定义或默认为"新对话"）
 * @param createdAt 会话创建时间
 * @param updatedAt 会话最后更新时间（每次添加消息时更新）
 */
public record ConversationSession(
        String id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
