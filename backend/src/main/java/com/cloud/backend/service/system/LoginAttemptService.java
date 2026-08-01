package com.cloud.backend.service.system;

import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败锁定服务 —— 防止暴力密码枚举。
 *
 * 设计思路：
 * 1. 每登录失败一次递增 Redis 计数器（TTL 可配置，默认 30 分钟），
 *    同一账号失败次数达到阈值（默认 5 次，可配置）后锁定一段时间（默认 30 分钟，可配置）
 * 2. 锁定期内 isLocked() 返回 true，AuthController 拒绝登录
 * 3. 登录成功后清除计数和锁
 *
 * 为什么不用数据库存计数？Redis 带 TTL 自过期，免清理，且性能更好。
 */
@Service
public class LoginAttemptService {

    private static final String ATTEMPT_PREFIX = "login:attempt:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redisTemplate;
    private final AdminSettingsService settingsService;

    public LoginAttemptService(StringRedisTemplate redisTemplate, AdminSettingsService settingsService) {
        this.redisTemplate = redisTemplate;
        this.settingsService = settingsService;
    }

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + username));
    }

    /** 登录失败时调用——累积失败次数，到达阈值则锁定 */
    public void loginFailed(String username) {
        int threshold = settingsService.getLoginLockThreshold();
        long lockDuration = settingsService.getLoginLockDurationMinutes();
        long attemptTtl = settingsService.getLoginAttemptTtlSeconds();
        String attemptKey = ATTEMPT_PREFIX + username;
        long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, attemptTtl, TimeUnit.SECONDS);
        }
        if (attempts >= threshold) {
            redisTemplate.opsForValue().set(LOCK_PREFIX + username, "1", lockDuration, TimeUnit.MINUTES);
            redisTemplate.delete(attemptKey);
        }
    }

    /** 登录成功后清除记录 */
    public void loginSucceeded(String username) {
        redisTemplate.delete(ATTEMPT_PREFIX + username);
        redisTemplate.delete(LOCK_PREFIX + username);
    }
}
