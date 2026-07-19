package com.ecommerce.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "创建商品请求")
public record ProductCreateRequest(

        @NotBlank(message = "商品标题不能为空")
        @Schema(description = "商品标题", example = "华为Mate 60 Pro")
        String title,

        @Schema(description = "商品描述", example = "鸿蒙系统，卫星通话")
        String description,

        @Schema(description = "分类ID", example = "1700000000000000001")
        Long categoryId,

        @NotNull(message = "价格不能为空")
        @Schema(description = "价格", example = "6999.00")
        BigDecimal price,

        @Schema(description = "主图URL", example = "https://example.com/image.jpg")
        String mainImage,

        @Schema(description = "搜索标签，逗号分隔", example = "手机,5G,旗舰")
        String searchTags
) {
}
