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
 *
 * 修改指引：
 * - 【习惯】修改暴露的认证管理器       → authenticationManager() @Bean 的返回内容；改动后影响登录流程 authenticate() 的认证能力
 * - 【习惯】修改认证逻辑（密码校验等）  → SecurityConfig 中的 AuthenticationProvider / DaoAuthenticationProvider；
 *                              此处仅转发 Spring 默认管理器，认证细节不在此类
 * - 【习惯】修改 Bean 名称/作用域       → @Bean 注解参数；改动后影响注入处（需同步 @Qualifier）
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
