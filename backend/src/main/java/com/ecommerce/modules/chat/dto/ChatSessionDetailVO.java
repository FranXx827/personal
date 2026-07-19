package com.ecommerce.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 会话 + 消息详情 */
@Data
@AllArgsConstructor
@Schema(description = "会话详情（含消息）")
public class ChatSessionDetailVO {
    private ChatSessionVO session;
    private List<ChatMessageVO> messages;
}
