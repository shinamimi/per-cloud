package com.cloud.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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

    public void loginSucceeded(String username) {
        redisTemplate.delete(ATTEMPT_PREFIX + username);
        redisTemplate.delete(LOCK_PREFIX + username);
    }
}