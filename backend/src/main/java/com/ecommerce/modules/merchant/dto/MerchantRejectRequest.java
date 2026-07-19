package com.ecommerce.modules.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "拒绝商户申请请求")
public record MerchantRejectRequest(

        @NotBlank(message = "拒绝原因不能为空")
        @Schema(description = "拒绝原因", example = "资质材料不完整")
        String reason
) {
}
