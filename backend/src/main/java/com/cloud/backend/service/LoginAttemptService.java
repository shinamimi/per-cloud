package com.cloud.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败锁定服务 —— 防止暴力密码枚举。
 *
 * 设计思路：
 * 1. 每登录失败一次递增 Redis 计数器（TTL=15分钟），同一账号 5 次失败后锁定 15 分钟
 * 2. 锁定期内 isLocked() 返回 true，AuthController 拒绝登录
 * 3. 登录成功后清除计数和锁
 *
 * 为什么不用数据库存计数？Redis 带 TTL 自过期，免清理，且性能更好。
 */
@Service
public class LoginAttemptService {

    private static final String ATTEMPT_PREFIX = "login:attempt:";
    private static final String LOCK_PREFIX = "login:lock:";

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + username));
    }

    /** 登录失败时调用——累积失败次数，到达阈值则锁定 */
    public void loginFailed(String username) {
        String attemptKey = ATTEMPT_PREFIX + username;
        long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(LOCK_PREFIX + username, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(attemptKey);
        }
    }

    /** 登录成功后清除记录 */
    public void loginSucceeded(String username) {
        redisTemplate.delete(ATTEMPT_PREFIX + username);
        redisTemplate.delete(LOCK_PREFIX + username);
    }
}