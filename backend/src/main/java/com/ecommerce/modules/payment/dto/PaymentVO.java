package com.ecommerce.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "支付视图")
public class PaymentVO {

    @Schema(description = "支付ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付状态")
    private String payStatus;
}
