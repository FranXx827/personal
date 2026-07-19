package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** 单条消息 */
@Data
@Schema(description = "单条消息")
public class ChatMessageVO {
    private String id;
    private String role;
    private String content;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
