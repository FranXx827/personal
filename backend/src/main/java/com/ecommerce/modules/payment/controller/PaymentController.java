package com.ecommerce.modules.payment.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.payment.dto.PaymentRequest;
import com.ecommerce.modules.payment.dto.PaymentVO;
import com.ecommerce.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付管理")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付")
    @PostMapping
    public Result<PaymentVO> createPayment(@Valid @RequestBody PaymentRequest req) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(paymentService.createPayment(req, userId));
    }

    @Operation(summary = "支付回调（第三方网关调用）")
    @PostMapping("/callback")
    public Result<PaymentVO> handlePaymentCallback(
            @RequestParam String transactionId,
            @RequestParam String payStatus,
            @RequestParam Long orderId) {
        return Result.success(paymentService.handlePaymentCallback(transactionId, payStatus, orderId));
    }

    @Operation(summary = "查询订单支付信息")
    @GetMapping("/order/{orderId}")
    public Result<PaymentVO> getPaymentByOrderId(@PathVariable Long orderId) {
        return Result.success(paymentService.getPaymentByOrderId(orderId));
    }
}
