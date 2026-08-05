package com.cloud.backend.service.system;

import com.cloud.backend.enums.CaptchaType;
import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务。
 *
 * 设计思路：
 * 1. 验证码使用 SecureRandom 生成 6 位数字，存入 Redis，过期时间可配置（session.captcha-ttl，默认 5 分钟）
 * 2. 验证码区分三种场景：注册(REGISTER)、登录(LOGIN)、重置密码(RESET_PASSWORD)
 *    通过 CaptchaType 区分 Key（如 captcha:REGISTER:xxx@email.com）
 * 3. 防刷机制：每个邮箱冷却期（mail.frequency-limit，默认 60 秒），冷却期内拒绝生成新验证码
 * 4. 验证通过后立即删除 Key，验证码只能使用一次
 *
 * 修改指引：
 * - 【习惯】想改"验证码位数/生成规则" → generateAndStore() 中 String.format("%06d", RANDOM.nextInt(1_000_000))；
 *   改动影响验证码强度与 Redis 校验
 * - 【习惯】想改"Redis Key 结构与场景隔离" → CAPTCHA_PREFIX 与 captchaKey()（captcha:{TYPE}:{email}）、
 *   verify(captchaId) 的 "captcha:LOGIN:" 拼接；改动须与 AuthServiceImpl/AuthController 传入的 captchaId 一致
 * - 【习惯】想改"验证码有效期" → generateAndStore() 中 settingsService.getCaptchaTtlSeconds()（默认 300 秒）；
 *   改动影响验证码过期窗口
 * - 【习惯】想改"冷却机制（时长/是否启用）" → isOnCooldown()/setCooldown() 与 settingsService.getMailFrequencyLimitSeconds()
 *   （默认 60 秒）；改动影响发信频率与防刷
 * - 【习惯】想改"验证一次有效" → verify() 中验证后 redisTemplate.delete(key)；改动影响验证码可复用性（安全）
 * - 【习惯】本类为具体实现类（@Service），非接口；被 AuthServiceImpl 直接注入调用
 */
@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String COOLDOWN_PREFIX = "captcha:cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final AdminSettingsService settingsService;

    public CaptchaService(StringRedisTemplate redisTemplate, AdminSettingsService settingsService) {
        this.redisTemplate = redisTemplate;
        this.settingsService = settingsService;
    }

    /** 生成 6 位验证码并存入 Redis */
    public String generateAndStore(String email, CaptchaType type) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = captchaKey(email, type);
        redisTemplate.opsForValue().set(key, code, settingsService.getCaptchaTtlSeconds(), TimeUnit.SECONDS);
        return code;
    }

    /** 验证码是否匹配（验证后删除，一次有效） */
    public boolean verify(String email, CaptchaType type, String code) {
        String key = captchaKey(email, type);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(key);
        return stored.equals(code);
    }

    /** 通过 captchaId 验证（登录场景，captchaId 作为 Redis Key） */
    public boolean verify(String captchaId, String code) {
        String key = CAPTCHA_PREFIX + "LOGIN:" + captchaId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(key);
        return stored.equals(code);
    }

    /** 生成验证码并存入 Redis，使用 captchaId 作为 Key */
    public String generateAndStore(String captchaId) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = CAPTCHA_PREFIX + "LOGIN:" + captchaId;
        redisTemplate.opsForValue().set(key, code, settingsService.getCaptchaTtlSeconds(), TimeUnit.SECONDS);
        return code;
    }

    /** 是否处于冷却期 */
    public boolean isOnCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email));
    }

    /** 设置冷却期 */
    public void setCooldown(String email) {
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1",
                settingsService.getMailFrequencyLimitSeconds(), TimeUnit.SECONDS);
    }

    private String captchaKey(String email, CaptchaType type) {
        return CAPTCHA_PREFIX + type.name() + ":" + email;
    }
}