package com.ecommerce.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品列表 VO（首页、搜索列表、热门推荐）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "商品列表视图")
public class ProductListVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "评分")
    private BigDecimal rating;
}
