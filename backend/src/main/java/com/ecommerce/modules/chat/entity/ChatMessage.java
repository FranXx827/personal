package com.ecommerce.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.INPUT)
    private String id;

    /** 会话ID */
    private String sessionId;

    /** user / assistant / system / tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用等附加信息的 JSON */
    private String metadataJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除 */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
