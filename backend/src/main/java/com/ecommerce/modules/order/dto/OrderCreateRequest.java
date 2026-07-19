package com.ecommerce.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建订单请求")
public record OrderCreateRequest(
        @Schema(description = "SKU ID")
        @NotNull Long skuId,

        @Schema(description = "数量")
        @Min(1) Integer quantity,

        @Schema(description = "收货人姓名")
        String receiverName,

        @Schema(description = "收货人电话")
        String receiverPhone,

        @Schema(description = "收货地址")
        String receiverAddress,

        @Schema(description = "备注")
        String remark
) {
}
