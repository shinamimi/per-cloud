package com.cloud.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器装配 —— 统一提供 BCrypt 密码编码器。
 *
 * 设计思路：
 * 1. 全项目共享同一个 PasswordEncoder Bean，保证注册、登录、重置密码的编码方式一致
 * 2. BCrypt 自带随机盐，同一明文每次编码结果不同，无需额外加盐逻辑
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 提供 BCrypt 实现，用于密码加密存储与登录比对。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
