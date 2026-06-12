package com.example.flight.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体 DTO（不可变记录类）。
 * <p>
 * 由 {@link AuthController#login} 接收并校验。
 * 使用 Jakarta Bean Validation 注解进行参数校验，
 * 校验失败时 Spring 会自动返回 400 错误及对应的 message 提示。
 * </p>
 *
 * @param username 登录用户名（2-32 个字符，不能为空）
 * @param password 登录密码（4-64 个字符，不能为空）
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名长度为2-32个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 4, max = 64, message = "密码长度为4-64个字符")
        String password
) {
}
