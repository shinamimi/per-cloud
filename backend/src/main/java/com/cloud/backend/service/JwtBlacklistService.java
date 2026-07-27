package com.cloud.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token 登出黑名单服务。
 *
 * 设计思路：
 * 实现"登出后 Token 立即失效"的效果。
 * 用户在主动登出时，将当前 Token 加入 Redis 黑名单，过期时间与 Token 剩余有效期相同。
 * 之后 JwtAuthenticationFilter 每次请求都会检查黑名单。
 *
 * 为什么不维护白名单？
 * 白名单的方案需要每次请求都刷新过期时间，对 Redis 压力更大。
 * 黑名单的 Key 自毁（TTL 到期），不需要额外清理。
 */
@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:jwt:";

    private final StringRedisTemplate redisTemplate;

    public JwtBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 将 Token 加入黑名单，过期时间与 Token 剩余有效期一致 */
    public void blacklistToken(String token, long expirationMs) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", expirationMs, TimeUnit.MILLISECONDS);
    }

    /** 检查 Token 是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}