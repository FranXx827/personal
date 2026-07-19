package com.ecommerce.infra.security;

/**
 * 当前请求用户上下文（ThreadLocal）
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        userIdHolder.set(userId);
        usernameHolder.set(username);
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    public static String getUsername() {
        return usernameHolder.get();
    }

    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
    }
}
