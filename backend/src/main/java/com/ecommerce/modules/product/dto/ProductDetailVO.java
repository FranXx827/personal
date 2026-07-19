package com.ecommerce.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品详情")
public class ProductDetailVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "主图")
    private String mainImage;

    @Schema(description = "SKU列表")
    private List<SkuVO> skus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "SKU视图")
    public static class SkuVO {

        @Schema(description = "SKU ID")
        private Long id;

        @Schema(description = "规格JSON")
        private String specs;

        @Schema(description = "库存")
        private Integer stock;

        @Schema(description = "价格")
        private BigDecimal price;
    }
}
