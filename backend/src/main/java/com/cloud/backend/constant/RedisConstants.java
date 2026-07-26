package com.cloud.backend.constant;

public interface RedisConstants {

    String JWT_BLACKLIST_PREFIX = "blacklist:jwt:";
    String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    String CAPTCHA_PREFIX = "captcha:";
    String USER_CACHE_PREFIX = "user:";
}