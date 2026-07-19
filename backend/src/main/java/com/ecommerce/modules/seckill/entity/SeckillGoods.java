package com.ecommerce.modules.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_goods")
public class SeckillGoods {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;

    private Long productId;

    private Integer seckillStock;

    private BigDecimal seckillPrice;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除 0-未删 1-已删（数据库 NOT NULL DEFAULT 0） */
    @TableLogic
    private Integer deleted;
}
