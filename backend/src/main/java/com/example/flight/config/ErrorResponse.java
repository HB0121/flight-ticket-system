package com.example.flight.config;

import java.time.LocalDateTime;

/**
 * 统一错误响应体，用于向客户端返回结构化的异常信息。
 * 使用 Java Record 实现不可变数据对象，自动生成构造器、getter、equals/hashCode/toString。
 *
 * @param status    HTTP 状态码（如 400、404、500、503）
 * @param error     错误类型简述（如 "参数校验失败"、"资源不存在"）
 * @param message   人类可读的错误详情
 * @param timestamp 错误发生时间（自动填充为当前时间）
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    /**
     * 便捷构造器：自动填充 timestamp 为当前时间。
     * 用于无需手动指定时间戳的场景。
     */
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now());
    }
}
