package com.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户信息（/auth/me 返回）
 */
@Schema(description = "用户信息")
public record UserInfoResponse(
        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "头像 URL")
        String avatar,

        @Schema(description = "角色列表", example = "[\"BUYER\"]")
        String[] roles
) {
}
