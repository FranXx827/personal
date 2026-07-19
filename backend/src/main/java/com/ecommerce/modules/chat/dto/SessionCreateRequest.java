package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建会话（自动由第一条消息触发） */
@Data
@Schema(description = "创建会话请求")
public class SessionCreateRequest {
    @NotBlank
    @Size(max = 64)
    @Schema(description = "会话ID (UUID)", example = "a1b2c3d4e5f6...")
    private String id;

    @Schema(description = "用户ID（由后端从 JWT 提取，前端无需传递）")
    private Long userId;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "会话标题", example = "帮我找一款拍照手机")
    private String title;
}
