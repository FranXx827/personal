package com.ecommerce.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    private String title;

    private String description;

    private Long categoryId;

    private BigDecimal price;

    /** 主图 URL */
    private String mainImage;

    private Integer sales;

    /** 评分 1.00-5.00 */
    private BigDecimal rating;

    /** 0-上架, 1-下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除 0-未删 1-已删（数据库 NOT NULL DEFAULT 0） */
    @TableLogic
    private Integer deleted;
}
