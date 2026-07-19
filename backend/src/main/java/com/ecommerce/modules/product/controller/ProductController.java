package com.ecommerce.modules.product.controller;

import com.ecommerce.common.response.PageResult;
import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.product.dto.*;
import com.ecommerce.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页搜索商品")
    @GetMapping
    public Result<PageResult<ProductListVO>> searchProducts(ProductPageRequest req) {
        return Result.success(productService.searchProducts(req));
    }

    @Operation(summary = "获取热门商品")
    @GetMapping("/hot")
    public Result<List<ProductListVO>> getHotProducts(
            @RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.getHotProducts(limit));
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getProductDetail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @Operation(summary = "创建商品（商家）")
    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductCreateRequest req) {
        Long merchantId = UserContextHolder.getUserId();
        Long productId = productService.createProduct(req, merchantId);
        return Result.success(productId);
    }
}
