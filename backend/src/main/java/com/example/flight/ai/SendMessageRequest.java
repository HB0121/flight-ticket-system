package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 500, message = "消息不能超过500字")
        String message
) {
}
