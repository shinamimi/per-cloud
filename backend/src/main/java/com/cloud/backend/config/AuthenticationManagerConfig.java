package com.cloud.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * 认证管理器装配 —— 将 Spring Security 的 AuthenticationManager 暴露为可注入的 Bean。
 *
 * 设计思路：
 * 1. 认证逻辑（密码校验、用户加载）沿用 SecurityConfig 中配置的 AuthenticationProvider
 * 2. 登录服务直接注入 AuthenticationManager 触发认证，不重复实现校验逻辑
 */
@Configuration
public class AuthenticationManagerConfig {

    /**
     * 提供认证管理器实例，供登录流程调用 authenticate() 完成认证。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
