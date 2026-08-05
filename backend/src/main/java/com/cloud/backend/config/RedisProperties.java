package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 配置映射。
 * Spring Boot 默认配置可用，这里自定义是为了支持从环境变量注入。
 *
 * 修改指引（yml 前缀 redis.）：
 * - 【习惯】host     → redis.host；默认 localhost；改动后影响 Redis 连接目标
 * - 【习惯】port     → redis.port；默认 6379；与 host 配套
 * - 【习惯】database → redis.database；默认 0（test 为 1）；改动后影响键所在逻辑库，换库需确认键冲突
 * - 【习惯】password → redis.password；默认为空；改动后影响认证
 * - 【习惯】timeout  → redis.timeout；单位毫秒；默认 3000；改动后影响连接超时判定
 */
@Data
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    private String host;
    private int port;
    private int database;
    private String password;
    private int timeout;
}