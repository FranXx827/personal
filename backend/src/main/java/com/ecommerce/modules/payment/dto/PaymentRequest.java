package com.ecommerce.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建支付请求")
public record PaymentRequest(
        @Schema(description = "订单ID")
        @NotNull Long orderId,

        @Schema(description = "支付方式: WECHAT, ALIPAY")
        @NotBlank String payMethod
) {
}
