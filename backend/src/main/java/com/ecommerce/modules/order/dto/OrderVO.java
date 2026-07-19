package com.ecommerce.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单视图")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "订单项列表")
    private List<OrderItemVO> items;

    @Data
    @AllArgsConstructor
    @Schema(description = "订单项视图")
    public static class OrderItemVO {

        @Schema(description = "SKU ID")
        private Long skuId;

        @Schema(description = "商品标题")
        private String productTitle;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "价格")
        private BigDecimal price;
    }
}
