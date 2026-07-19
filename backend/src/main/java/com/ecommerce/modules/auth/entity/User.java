package com.ecommerce.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（买家/商户统一存储）
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String phone;

    private String avatar;

    /** BUYER / MERCHANT */
    private String role;

    /** 0-正常 1-禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除 0-未删 1-已删（数据库 NOT NULL DEFAULT 0） */
    @TableLogic
    private Integer deleted;
}
