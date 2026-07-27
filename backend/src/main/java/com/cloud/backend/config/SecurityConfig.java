package com.cloud.backend.config;

import com.cloud.backend.security.AccessDeniedHandlerImpl;
import com.cloud.backend.security.AuthenticationEntryPointImpl;
import com.cloud.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置中心，是整个认证体系的核心编排器。
 *
 * 设计思路：
 * 1. 使用 JWT 无状态认证，因此关闭 CSRF 和 Session，每次请求通过 Token 鉴权
 * 2. 按角色粒度划分接口权限：SUPER_ADMIN > ADMIN > 已登录用户
 * 3. 自定义 JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前，先解析 Token 再校验账号密码
 * 4. CORS 全放通，前端开发时通过 Vite Proxy 转发，生产环境由 Nginx 处理
 */
@Configuration
public class SecurityConfig {

    /** 自定义 JWT 拦截过滤器，解析请求头的 Token、封装登录用户上下文 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    /** 未登录 / Token 无效全局异常处理器 */
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    /** 已登录但无权限全局异常处理器 */
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationEntryPointImpl authenticationEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /** 密码加密器 PasswordEncoder —— 使用 BCrypt 单向哈希，不可逆，防止数据库泄露后密码被还原 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露 AuthenticationManager，供 AuthController 在登录时手动调用 authenticate()
     * 这样可以在登录逻辑中自定义错误处理（账号锁定、失败计数等）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 安全过滤链 SecurityFilterChain —— Spring Security 6+ 的配置方式
     *
     * 配置步骤说明：
     * 1. 关闭 CSRF：JWT 无状态，CSRF 防护不再必要
     * 2. 开启跨域，绑定下方自定义跨域配置
     * 3. 会话策略：无状态，禁用 Session
     * 4. 统一认证/权限异常处理
     * 5. 接口放行/鉴权规则 —— 由粗到细
     * 6. 在账号密码校验过滤器之前执行 JWT 过滤器
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 配置 —— 允许所有来源和方法
     * 实现原理：Spring 的 CorsConfigurationSource 会针对每个请求检查 Origin 头，
     * 匹配后添加 CORS 响应头（Access-Control-Allow-Origin 等）。
     * 开发时由 Vite Proxy 转发，此配置主要用于生产环境或直接用 Swagger 测试。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}