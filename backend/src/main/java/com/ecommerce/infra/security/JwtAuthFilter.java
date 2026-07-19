package com.ecommerce.infra.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Value("${service.token}")
    private String serviceToken;

    private static final Long SERVICE_USER_ID = 0L;
    private static final String SERVICE_USERNAME = "ai-assistant";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            Claims claims = jwtProvider.parseAccessToken(token);
            if (claims != null) {
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);

                String authority = role != null ? "ROLE_" + role : "ROLE_USER";
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new AuthUser(userId, username, role),
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserContextHolder.set(userId, username);
            }
        } else {
            // 无 JWT 时尝试服务间鉴权 (X-Service-Token)
            String serviceToken = request.getHeader("X-Service-Token");
            if (StringUtils.hasText(serviceToken) && serviceToken.equals(this.serviceToken)) {
                UserContextHolder.set(SERVICE_USER_ID, SERVICE_USERNAME);
                log.debug("Service token auth success: userId={}", SERVICE_USER_ID);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清除上下文
            UserContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
