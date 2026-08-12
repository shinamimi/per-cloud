package com.cloud.backend.utils;

import com.cloud.backend.config.JwtProperties;
import com.cloud.backend.security.LoginUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具实现 —— 基于 jjwt 的签发、解析与校验。
 *
 * 设计思路：
 * 1. 密钥在构造时由配置的 Base64 密钥派生（HMAC-SHA），全生命周期复用同一实例
 * 2. 解析时强制校验签发者（issuer），防止跨环境 Token 混用
 * 3. 过期时间不读固定配置而是实时取管理端设置（getAccessTokenTtlMs），
 *    调整配置后新签发的 Token 立即生效
 *
 * 修改指引：
 * - 【统一】修改签名算法/密钥       → 构造器中 Keys.hmacShaKeyFor(...)；密钥来自 JwtProperties.secret，
 *                             更换密钥会使存量 Token 全部失效（所有用户需重新登录）；
 *                             改后需同步 JwtProperties.secret 配置（密钥格式/长度与算法匹配）与存量 Token 失效评估
 * - 【统一】修改签发 claim          → generateToken 中的 subject / role claim；须与 getUsernameFromToken / getRoleFromToken 解析保持一致；
 *                             改后需同步解析逻辑（getUsernameFromToken / getRoleFromToken）
 * - 【统一】修改签发者校验          → parseClaims 中 requireIssuer(jwtProperties.getIssuer())；改动影响跨环境 Token 混用防护；
 *                             改后需同步 generateToken 签发的 issuer 与 JwtProperties.issuer 配置
 * - 【习惯】修改有效期来源          → getExpirationMs；当前实时读管理端配置 getAccessTokenTtlMs，改动后新签发 Token 立即生效
 */
@Component
public class JwtTokenUtilImpl implements JwtTokenUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final com.cloud.backend.service.admin.AdminSettingsService settingsService;

    public JwtTokenUtilImpl(JwtProperties jwtProperties,
                            com.cloud.backend.service.admin.AdminSettingsService settingsService) {
        this.jwtProperties = jwtProperties;
        this.settingsService = settingsService;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /**
     * 签发 JWT：subject 为用户名字符串，role 以 Role.value 存入 claim，
     * 有效期按当前管理端配置计算，签名使用构造时派生的密钥。
     */
    @Override
    public String generateToken(LoginUser loginUser) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claim("role", loginUser.getRole().getValue())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + getExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token 并返回 subject（用户名）。
     */
    @Override
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 解析 Token 并返回 role claim（角色值）。
     */
    @Override
    public Integer getRoleFromToken(String token) {
        return parseClaims(token).get("role", Integer.class);
    }

    /**
     * 校验 Token：签名正确且未过期（签发者不匹配视为非法）返回 true；否则返回 false。
     * 任何解析异常统一吞掉，对外只表达"是否有效"，不泄露原因。
     */
    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 当前 Token 有效期（毫秒），实时取管理端配置。
     */
    @Override
    public long getExpirationMs() {
        return settingsService.getAccessTokenTtlMs();
    }

    /**
     * 解析并校验 Token：验签 + 校验签发者与过期时间，返回 payload。
     * 签名非法、过期、签发者不符或 Token 格式错误时抛 JwtException。
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
