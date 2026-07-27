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
 * JWT 令牌操作工具。
 *
 * 设计思路：
 * 1. 使用 JJWT 库，构造函数中从配置读取 Base64 编码的密钥并生成 HMAC-SHA 密钥
 * 2. generateToken：将用户名作为 subject，角色信息作为 claim 放入 payload
 * 3. validateToken：验签、校验过期时间和 issuer
 * 4. 抛出 JwtException 说明 Token 无效（过期、篡改、格式错误）
 *
 * 为什么不在 Token 里放 userId？
 * 因为 user_id 需要查数据库才可获得，而 SecurityContext 中 LoginUser 持有 userId，
 * 通过 username 查一次 UserDetailsService 即可拿到完整信息，不需要额外声明 claim。
 */
@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /** 生成 JWT Token */
    public String generateToken(LoginUser loginUser) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claim("role", loginUser.getRole().getValue())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Integer getRoleFromToken(String token) {
        return parseClaims(token).get("role", Integer.class);
    }

    /** 校验 Token 是否有效（签名正确且未过期） */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 获取 Token 过期时间（毫秒），用于黑名单服务设置相同的过期时间 */
    public long getExpirationMs() {
        return jwtProperties.getExpiration();
    }

    /** 解析 JWT Claims，包含验签和 issuer 校验 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}