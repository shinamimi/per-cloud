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

    /** 【统一】改后需同步 yml redis.host+读取方(暂未直接注入使用，Spring Boot 自动配置通过 spring.data.redis.*)（无单位，主机名或 IP） */
    private String host;
    /** 【统一】改后需同步 yml redis.port+读取方(暂未直接注入使用，Spring Boot 自动配置通过 spring.data.redis.*)（无单位，端口号） */
    private int port;
    /** 【统一】改后需同步 yml redis.database+读取方(暂未直接注入使用，Spring Boot 自动配置通过 spring.data.redis.*)（无单位，数据库序号） */
    private int database;
    /** 【统一】改后需同步 yml redis.password+读取方(暂未直接注入使用，Spring Boot 自动配置通过 spring.data.redis.*)（无单位，密码字符串） */
    private String password;
    /** 【统一】改后需同步 yml redis.timeout+读取方(暂未直接注入使用，Spring Boot 自动配置通过 spring.data.redis.*)（毫秒） */
    private int timeout;
}