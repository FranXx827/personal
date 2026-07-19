package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 添加消息 */
@Data
@Schema(description = "添加消息请求")
public class MessageCreateRequest {
    @NotBlank
    @Size(max = 64)
    @Schema(description = "消息ID (UUID)")
    private String id;

    @NotBlank
    @Size(max = 16)
    @Schema(description = "角色: user / assistant / system / tool")
    private String role;

    @Schema(description = "消息内容")
    private String content = "";

    @Schema(description = "会话ID（由控制器路径变量注入，请求体无需传递）")
    private String sessionId;

    @Schema(description = "工具调用等附加信息的 JSON 字符串")
    private String metadataJson;
}
