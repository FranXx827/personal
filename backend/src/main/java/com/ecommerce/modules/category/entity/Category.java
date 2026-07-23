package com.ecommerce.modules.category.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分类名称，如"手机"、"智能手机" */
    private String name;

    /** 父分类ID，顶级分类为NULL */
    private Long parentId;

    /** 层级：1-顶级 2-二级 3-三级 */
    private Integer level;

    /** 排序号（同级排序） */
    private Integer sortOrder;

    /** 子分类列表（非数据库字段，仅用于树形展示） */
    @TableField(exist = false)
    private List<Category> children;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
