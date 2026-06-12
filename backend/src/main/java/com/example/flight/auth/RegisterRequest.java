package com.example.flight.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体 DTO（不可变记录类）。
 * <p>
 * 由 {@link AuthController#register} 接收并校验。
 * 校验规则与 {@link LoginRequest} 相同：用户名 2-32 字符，密码 4-64 字符。
 * 注册成功后自动返回登录令牌，无需再单独登录。
 * </p>
 *
 * @param username 注册用户名（2-32 个字符，不能为空，必须全局唯一）
 * @param password 注册密码（4-64 个字符，不能为空）
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名长度为2-32个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 4, max = 64, message = "密码长度为4-64个字符")
        String password
) {
}
