package com.ecommerce.modules.seckill.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "秒杀结果")
public record SeckillResult(
        @Schema(description = "是否成功") Boolean success,
        @Schema(description = "提示消息") String message,
        @Schema(description = "订单号") String orderNo
) {
}
