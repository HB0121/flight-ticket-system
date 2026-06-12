package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求体 —— POST /api/ai/conversations/{sessionId}/messages 的请求 DTO。
 *
 * 用于在多轮对话中发送新的用户消息，系统会自动生成 AI 回复并记录到对话历史中。
 *
 * @param message 用户消息文本内容
 *                必填，最大长度 500 字符
 */
public record SendMessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 500, message = "消息不能超过500字")
        String message
) {
}
