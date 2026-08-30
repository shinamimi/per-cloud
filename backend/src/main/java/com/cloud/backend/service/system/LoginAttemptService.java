package com.cloud.backend.service.system;

import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
