package com.cloud.backend.service.system;

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
 *
 * 修改指引：
 * - 【习惯】想改"黑名单 Key 前缀/结构" → BLACKLIST_PREFIX（"blacklist:jwt:"）与 blacklistToken()/isBlacklisted()；
 *   改动须与 JwtAuthenticationFilter 的检查点同步
 * - 【习惯】想改"黑名单过期时间" → blacklistToken(token, expirationMs) 的 expirationMs 入参（调用方传 Token 剩余
 *   有效期）；改动影响登出后 Token 失效窗口
 * - 【习惯】想改"失效策略（如改白名单/分布式共享）" → 本类整体语义变化；改动影响登出场景与过滤器开销
 * - 【习惯】本类为具体实现类（@Service），非接口；被 AuthController（登出）/JwtAuthenticationFilter 调用
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