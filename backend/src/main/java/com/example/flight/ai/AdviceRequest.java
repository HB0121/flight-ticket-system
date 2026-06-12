package com.example.flight.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdviceRequest(
        @NotBlank(message = "出行需求描述不能为空")
        @Size(max = 500, message = "出行需求描述不能超过500字")
        String message
) {
}

