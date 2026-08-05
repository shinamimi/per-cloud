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
 *
 * 修改指引：
 * - 【习惯】想改"失败计数 TTL/锁定阈值/锁定时长" → loginFailed() 中 settingsService.getLoginAttemptTtlSeconds()/
 *   getLoginLockThreshold()/getLoginLockDurationMinutes()；改动影响锁定策略与 Redis 占用
 * - 【习惯】想改"计数与锁的 Key 结构" → ATTEMPT_PREFIX（"login:attempt:"）/LOCK_PREFIX（"login:lock:"）；
 *   改动须与 AuthServiceImpl/AuthController 调用保持一致
 * - 【习惯】想改"锁定期自动解锁" → isLocked() 返回与 AuthServiceImpl.login() 顶部"已过窗口先解除 LOCKED"逻辑；
 *   改动影响锁定/解锁流转
 * - 【习惯】并发注意：loginFailed() 用 Redis increment() 原子累加（首次设置 TTL）；勿改读改写，否则并发登录失败计数错乱
 * - 【习惯】本类为具体实现类（@Service），非接口；被 AuthServiceImpl/AuthController 调用
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
