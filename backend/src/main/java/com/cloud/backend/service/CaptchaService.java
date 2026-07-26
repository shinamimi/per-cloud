package com.cloud.backend.service;

import com.cloud.backend.enums.CaptchaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String COOLDOWN_PREFIX = "captcha:cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final long CAPTCHA_TTL_SECONDS = 300;
    private static final long COOLDOWN_TTL_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public CaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndStore(String email, CaptchaType type) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = captchaKey(email, type);
        redisTemplate.opsForValue().set(key, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);
        return code;
    }

    public boolean verify(String email, CaptchaType type, String code) {
        String key = captchaKey(email, type);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(key);
        return stored.equals(code);
    }

    public boolean isOnCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email));
    }

    public void setCooldown(String email) {
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", COOLDOWN_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String captchaKey(String email, CaptchaType type) {
        return CAPTCHA_PREFIX + type.name() + ":" + email;
    }
}