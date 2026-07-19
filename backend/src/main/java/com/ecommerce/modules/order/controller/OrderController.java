package com.ecommerce.modules.order.controller;

import com.ecommerce.common.response.PageResult;
import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.order.dto.OrderCreateRequest;
import com.ecommerce.modules.order.dto.OrderVO;
import com.ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateRequest req) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(orderService.createOrder(req, userId));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(orderService.getOrderDetail(id, userId));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        orderService.payOrder(id, userId);
        return Result.success();
    }

    @Operation(summary = "查询用户订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> listUserOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(orderService.listUserOrders(userId, status, page, pageSize));
    }
}
