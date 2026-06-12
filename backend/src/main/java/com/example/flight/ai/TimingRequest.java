package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 购票时机分析请求体 —— POST /api/ai/timing 的请求 DTO。
 *
 * 用户通过自然语言描述出行计划，系统分析最佳购票时机。
 * 消息中应包含城市、日期等关键信息，由 {@link TravelIntentParser} 统一解析。
 *
 * @param message 用户购票时机问题描述（如"什么时候买北京到上海的机票最便宜？"）
 *                必填，最大长度 500 字符
 */
public record TimingRequest(
        @NotBlank(message = "购票时机问题不能为空")
        @Size(max = 500, message = "购票时机问题不能超过500字")
        String message
) {
}
