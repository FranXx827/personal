package com.ecommerce.infra.security;

import com.ecommerce.modules.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 的 {@link UserDetails} 实现，包装领域模型 {@link User}。
 * 作为认证主体（Principal）在登录与鉴权中统一承载用户信息：
 * 权限取自角色（ROLE_xxx），账号是否可用由 status 字段决定。
 */
@Getter
public class SecurityUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final String password;
    private final String role;
    private final boolean enabled;

    public SecurityUser(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.password = user.getPasswordHash();
        this.role = user.getRole();
        // status = 1 表示禁用；null 视为正常
        this.enabled = user.getStatus() == null || user.getStatus() == 0;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = role != null ? "ROLE_" + role : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
