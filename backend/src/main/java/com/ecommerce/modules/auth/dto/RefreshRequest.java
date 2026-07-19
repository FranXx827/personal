package com.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record RefreshRequest(

        @NotBlank(message = "刷新令牌不能为空")
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken
) {
}
