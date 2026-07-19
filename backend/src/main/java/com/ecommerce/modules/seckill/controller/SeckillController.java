package com.ecommerce.modules.seckill.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.seckill.dto.SeckillResult;
import com.ecommerce.modules.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "秒杀管理")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "秒杀下单")
    @PostMapping("/{skuId}")
    public Result<SeckillResult> seckill(@PathVariable Long skuId) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(seckillService.seckill(skuId, userId));
    }
}
