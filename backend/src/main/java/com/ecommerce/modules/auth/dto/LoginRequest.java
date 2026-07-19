package com.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "登录请求")
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Schema(description = "用户名", example = "buyer1")
        String username,

        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码", example = "123456")
        String password,

        @Schema(description = "登录类型: BUYER / MERCHANT", example = "BUYER")
        String type
) {
}
