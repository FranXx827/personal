package com.ecommerce.modules.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "商户入驻申请请求")
public record MerchantApplyRequest(

        @NotBlank(message = "店铺名称不能为空")
        @Schema(description = "店铺名称", example = "数码旗舰店")
        String name,

        @NotBlank(message = "店铺描述不能为空")
        @Schema(description = "店铺描述", example = "主营各类数码电子产品")
        String description,

        @NotBlank(message = "联系电话不能为空")
        @Schema(description = "联系电话", example = "13800138000")
        String contactPhone
) {
}
