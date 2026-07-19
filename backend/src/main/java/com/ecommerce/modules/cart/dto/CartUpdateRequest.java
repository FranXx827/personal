package com.ecommerce.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新购物车项请求")
public record CartUpdateRequest(

        @Schema(description = "数量", example = "2")
        Integer quantity,

        @Schema(description = "是否选中", example = "true")
        Boolean selected
) {
}
