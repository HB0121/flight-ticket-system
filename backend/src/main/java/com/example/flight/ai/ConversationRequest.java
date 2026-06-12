package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建对话请求体 —— POST /api/ai/conversations 的请求 DTO。
 *
 * @param title 会话标题（可选，为空时默认使用"新对话"）
 */
public record ConversationRequest(
        String title
) {
}
