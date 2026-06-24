package com.example.flight.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名长度为2-32个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 4, max = 64, message = "密码长度为4-64个字符")
        String password
) {
}
