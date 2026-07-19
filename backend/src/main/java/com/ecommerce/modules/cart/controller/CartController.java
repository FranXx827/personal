package com.ecommerce.modules.cart.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.cart.dto.CartAddRequest;
import com.ecommerce.modules.cart.dto.CartUpdateRequest;
import com.ecommerce.modules.cart.dto.CartVO;
import com.ecommerce.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车管理")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "获取购物车列表")
    @GetMapping
    public Result<List<CartVO>> getCart() {
        Long userId = UserContextHolder.getUserId();
        List<CartVO> cart = cartService.getCart(userId);
        return Result.success(cart);
    }

    @Operation(summary = "添加商品到购物车")
    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody CartAddRequest req) {
        Long userId = UserContextHolder.getUserId();
        cartService.addItem(req, userId);
        return Result.success();
    }

    @Operation(summary = "更新购物车项")
    @PatchMapping("/items/{itemId}")
    public Result<Void> updateItem(@PathVariable Long itemId,
                                   @Valid @RequestBody CartUpdateRequest req) {
        Long userId = UserContextHolder.getUserId();
        cartService.updateItem(itemId, req, userId);
        return Result.success();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        Long userId = UserContextHolder.getUserId();
        cartService.deleteItem(itemId, userId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clearCart() {
        Long userId = UserContextHolder.getUserId();
        cartService.clearCart(userId);
        return Result.success();
    }
}
