package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新会话标题 */
@Data
@Schema(description = "更新会话标题请求")
public class TitleUpdateRequest {
    @NotBlank
    @Size(max = 200)
    @Schema(description = "新标题")
    private String title;
}
