package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 会话列表项 */
@Data
@Schema(description = "会话列表项")
public class ChatSessionVO {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer messageCount;
}
