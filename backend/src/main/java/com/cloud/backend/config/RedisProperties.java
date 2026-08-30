package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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