package com.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;

    @Schema(description = "用户ID", example = "1700000000000000001")
    private Long userId;

    @Schema(description = "用户名", example = "buyer1")
    private String username;

    @Schema(description = "昵称", example = "买家小明")
    private String nickname;

    @Schema(description = "角色", example = "BUYER")
    private String role;
}
