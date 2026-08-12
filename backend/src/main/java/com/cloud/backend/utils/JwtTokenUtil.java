package com.cloud.backend.utils;

import com.cloud.backend.security.LoginUser;

/**
 * JWT 工具接口 —— 登录态 Token 的签发、解析与校验。
 *
 * 设计思路：
 * 1. Token 无状态签发，身份信息存于 claim（用户名 + 角色值），服务端不维护会话
 * 2. 过期时间动态取自管理端配置（getExpirationMs），用于签发与黑名单清理
 *
 * 修改指引：
 * - 【统一】新增 Token 能力方法     → 在此接口声明并在 JwtTokenUtilImpl 实现；涉及加解密逻辑的修改需评估安全性；
 *                             改后需同步 JwtTokenUtilImpl 实现
 * - 【统一】修改 claim 结构         → generateToken / getUsernameFromToken / getRoleFromToken；改动后新旧 Token 兼容性需评估；
 *                             改后需同步签发（generateToken）与解析（getUsernameFromToken/getRoleFromToken）保持一致
 * - 【统一】修改校验语义            → validateToken；当前非法/过期返回 false 不抛异常，改动影响过滤器等调用方；
 *                             改后需同步过滤器等调用方
 */
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
