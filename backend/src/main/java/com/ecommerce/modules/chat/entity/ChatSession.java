package com.ecommerce.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话会话
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.INPUT)
    private String id;

    /** 用户ID */
    private Long userId;

    /** 会话标题 */
    private String title;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除时间戳: 0=正常, >0=已删除 */
    @TableLogic(value = "0", delval = "UNIX_TIMESTAMP()")
    private Long deleted;
}
