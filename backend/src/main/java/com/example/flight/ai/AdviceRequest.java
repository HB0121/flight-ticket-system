package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 出行建议请求体 —— POST /api/ai/advice 的请求 DTO。
 *
 * 包含用户的自然语言出行需求描述，由服务端解析为结构化查询参数。
 * 使用 Jakarta Bean Validation 注解保证输入合法性。
 *
 * @param message 用户出行需求描述（如"从上海到北京后天出发，预算800元"）
 *                必填，最大长度 500 字符
 */
public record AdviceRequest(
        @NotBlank(message = "出行需求描述不能为空")
        @Size(max = 500, message = "出行需求描述不能超过500字")
        String message
) {
}
