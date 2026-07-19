package com.ecommerce.modules.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;        // 关联用户
    private String name;        // 店铺名称
    private String description; // 店铺描述
    private String contactPhone;
    private String contactEmail;
    private String auditStatus;  // PENDING/APPROVED/REJECTED
    private String rejectReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除 0-未删 1-已删（数据库 NOT NULL DEFAULT 0） */
    @TableLogic
    private Integer deleted;
}
