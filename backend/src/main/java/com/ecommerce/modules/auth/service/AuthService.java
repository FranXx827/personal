package com.ecommerce.modules.auth.service;

import com.ecommerce.modules.auth.dto.*;

public interface AuthService {

    LoginResponse login(LoginRequest req);

    void register(RegisterRequest req);

    LoginResponse refresh(String refreshToken);

    UserInfoResponse getCurrentUser(Long userId);

    /**
     * 登出（预留清理逻辑）
     */
    void logout(Long userId);
}
