package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TimingRequest(
        @NotBlank(message = "购票时机问题不能为空")
        @Size(max = 500, message = "购票时机问题不能超过500字")
        String message
) {
}
