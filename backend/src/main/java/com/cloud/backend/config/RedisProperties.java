package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 配置映射。
 * Spring Boot 默认配置可用，这里自定义是为了支持从环境变量注入。
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