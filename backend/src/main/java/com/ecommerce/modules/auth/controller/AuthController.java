package com.ecommerce.modules.auth.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.auth.dto.*;
import com.ecommerce.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register/{type}")
    public Result<Void> register(@PathVariable String type,
                                  @Valid @RequestBody RegisterRequest request) {
        RegisterRequest adjusted = new RegisterRequest(
                request.username(),
                request.password(),
                request.name(),
                request.merchantName(),
                request.phone(),
                type
        );
        authService.register(adjusted);
        return Result.success();
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.error(401, "缺少刷新令牌");
        }
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(authService.getCurrentUser(userId));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContextHolder.getUserId();
        authService.logout(userId);
        return Result.success();
    }
}
