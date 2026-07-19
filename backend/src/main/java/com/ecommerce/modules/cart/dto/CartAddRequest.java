package com.ecommerce.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "添加购物车请求")
public record CartAddRequest(

        @NotNull(message = "SKU ID不能为空")
        @Schema(description = "SKU ID", example = "1700000000000000001")
        Long skuId,

        @Min(value = 1, message = "数量至少为1")
        @Schema(description = "数量", example = "1")
        Integer quantity
) {
}
