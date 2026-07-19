package com.ecommerce.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.enums.ResultCode;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.DuplicateResourceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.infra.security.JwtProvider;
import com.ecommerce.modules.auth.dto.*;
import com.ecommerce.modules.auth.entity.User;
import com.ecommerce.modules.auth.mapper.UserMapper;
import com.ecommerce.modules.auth.service.AuthService;
import com.ecommerce.modules.merchant.entity.Merchant;
import com.ecommerce.modules.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.username())
        );
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 登录只校验凭证（用户名+密码）。角色由用户记录决定，并通过 JWT 下发给前端，
        // 不应依赖客户端传入的 type 做身份校验——否则选错登录类型（默认 BUYER）会被误判为「未登录」。
        // 前端会根据 /auth/me 返回的角色自行路由到对应门户。

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.username())
        );
        if (count > 0) {
            throw new DuplicateResourceException("用户名已存在");
        }

        String role = req.type() != null ? req.type() : "BUYER";

        // 买家注册时 name 为昵称，商户注册时 merchantName 为商户名，nickname 取 name 或用户名
        String nickname = "BUYER".equals(role) ? req.name() : req.merchantName();
        if (nickname == null || nickname.isBlank()) {
            nickname = req.username();
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(0);
        if (req.phone() != null && !req.phone().isBlank()) {
            user.setPhone(req.phone().trim());
        }
        userMapper.insert(user);

        // 商户注册时同时创建 merchant 记录（默认待审核）
        if ("MERCHANT".equals(role) && req.merchantName() != null && !req.merchantName().isBlank()) {
            Merchant merchant = new Merchant();
            merchant.setUserId(user.getId());
            merchant.setName(req.merchantName());
            merchant.setAuditStatus("PENDING");
            merchantMapper.insert(merchant);
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        var claims = jwtProvider.parseRefreshToken(refreshToken);
        if (claims == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        // 从数据库获取最新用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, username, user.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .username(username)
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户", userId);
        }
        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                new String[]{user.getRole()}
        );
    }

    @Override
    public void logout(Long userId) {
        log.info("用户 {} 登出", userId);
        // 预留：清理 token 黑名单、刷新 token 失效等
    }
}
