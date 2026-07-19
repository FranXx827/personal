package com.ecommerce.modules.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "商户信息")
public class MerchantVO {

    @Schema(description = "商户ID", example = "1700000000000000001")
    private Long id;

    @Schema(description = "店铺名称", example = "数码旗舰店")
    private String name;

    @Schema(description = "店铺描述", example = "主营各类数码电子产品")
    private String description;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

    @Schema(description = "拒绝原因", example = "资质材料不完整")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
