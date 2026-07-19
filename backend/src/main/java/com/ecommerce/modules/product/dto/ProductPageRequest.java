package com.ecommerce.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "商品分页查询请求")
public record ProductPageRequest(

        @Schema(description = "页码", example = "1")
        Integer page,

        @Schema(description = "每页条数", example = "20")
        Integer pageSize,

        @Schema(description = "关键词搜索", example = "手机")
        String keyword,

        @Schema(description = "分类ID", example = "1700000000000000001")
        Long categoryId,

        @Schema(description = "排序字段", example = "price")
        String sortBy,

        @Schema(description = "排序方向", example = "asc")
        String sortDir
) {
}
