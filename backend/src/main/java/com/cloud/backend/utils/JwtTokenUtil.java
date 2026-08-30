package com.cloud.backend.utils;

import com.cloud.backend.security.LoginUser;

public interface JwtTokenUtil {

    /**
     * 为用户签发 JWT，包含用户名（subject）与角色值（role claim）。
     */
    String generateToken(LoginUser loginUser);

    /**
     * 从 Token 解析用户名；Token 非法/过期时抛 JwtException。
     */
    String getUsernameFromToken(String token);

    /**
     * 从 Token 解析角色值（Role.value）；Token 非法/过期时抛 JwtException。
     */
    Integer getRoleFromToken(String token);

    /**
     * 校验 Token 签名与过期时间，非法（含过期）返回 false，不抛异常。
     */
    boolean validateToken(String token);

    /**
     * Token 有效期（毫秒），来自管理端配置。
     */
    long getExpirationMs();
}
