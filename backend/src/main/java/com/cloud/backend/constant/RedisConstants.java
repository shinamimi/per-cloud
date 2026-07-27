package com.cloud.backend.constant;

/**
 * Redis Key 常量 —— 集中管理所有 Redis Key 前缀，避免散落在各处。
 */
public interface RedisConstants {

    String JWT_BLACKLIST_PREFIX = "blacklist:jwt:";
    String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    String CAPTCHA_PREFIX = "captcha:";
    String USER_CACHE_PREFIX = "user:";
}