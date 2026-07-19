package com.ecommerce.infra.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.modules.auth.entity.User;
import com.ecommerce.modules.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户，供 {@code DaoAuthenticationProvider} 做凭证校验与账号状态判断。
 * 定义该 Bean 后，Spring Boot 的 UserDetailsServiceAutoConfiguration 会退出，
 * 不再生成随机会话密码。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return new SecurityUser(user);
    }
}
