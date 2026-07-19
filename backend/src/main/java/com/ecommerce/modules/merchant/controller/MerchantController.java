package com.ecommerce.modules.merchant.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.merchant.dto.MerchantApplyRequest;
import com.ecommerce.modules.merchant.dto.MerchantRejectRequest;
import com.ecommerce.modules.merchant.dto.MerchantVO;
import com.ecommerce.modules.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商户管理")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "申请入驻商户")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody MerchantApplyRequest request) {
        merchantService.apply(request, UserContextHolder.getUserId());
        return Result.success();
    }

    @Operation(summary = "获取审核列表")
    @GetMapping("/audit")
    public Result<List<MerchantVO>> getAuditList(
            @Parameter(description = "审核状态(PENDING/APPROVED/REJECTED)")
            @RequestParam(required = false) String auditStatus) {
        return Result.success(merchantService.getAuditList(auditStatus));
    }

    @Operation(summary = "通过商户审核")
    @PostMapping("/{id}/approve")
    public Result<MerchantVO> approve(
            @Parameter(description = "商户ID")
            @PathVariable Long id) {
        return Result.success(merchantService.approve(id));
    }

    @Operation(summary = "拒绝商户审核")
    @PostMapping("/{id}/reject")
    public Result<MerchantVO> reject(
            @Parameter(description = "商户ID")
            @PathVariable Long id,
            @Valid @RequestBody MerchantRejectRequest request) {
        return Result.success(merchantService.reject(id, request.reason()));
    }

    @Operation(summary = "获取我的商户信息")
    @GetMapping("/my")
    public Result<MerchantVO> getMyMerchant() {
        return Result.success(merchantService.getMyMerchant(UserContextHolder.getUserId()));
    }
}
