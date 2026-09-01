package com.cloud.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置。
 *
 * 设计思路：
 * 1. 为热点查询（File/User/Share findById）提供本地缓存，减少 DB 查询
 * 2. 30s TTL 足够应对读多写少场景
 * 3. 最大 10000 条，防止内存溢出
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }
}
