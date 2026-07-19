package com.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 64, message = "用户名长度 2-64")
        @Schema(description = "登录用户名", example = "newuser")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度 6-64")
        @Schema(description = "密码", example = "123456")
        String password,

        @Schema(description = "昵称（买家注册时用）", example = "新用户")
        String name,

        @Schema(description = "商户名称（商户注册时用）", example = "我的店铺")
        String merchantName,

        @Schema(description = "手机号（选填，用于订单通知）", example = "13800138000")
        String phone,

        @NotBlank(message = "注册类型不能为空")
        @Schema(description = "注册类型: BUYER / MERCHANT", example = "BUYER")
        String type
) {
}
