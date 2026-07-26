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

@Configuration
public class SecurityConfig {

    /*JwtAuthenticationFilter：自定义 JWT 拦截过滤器，解析请求头的 Token、封装登录用户上下文
    AuthenticationEntryPointImpl：未登录 / Token 无效全局异常处理器
    AccessDeniedHandlerImpl：已登录但无权限全局异常处理器 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationEntryPointImpl authenticationEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); /* 密码加密器 PasswordEncoder */
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1.关闭CSRF防护
                .csrf(AbstractHttpConfigurer::disable)
                // 2.开启跨域，绑定下方自定义跨域配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3.会话策略：无状态，禁用Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4.统一认证/权限异常处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint) // 未登录、Token过期/非法
                        .accessDeniedHandler(accessDeniedHandler) // 登录成功，但没有接口访问权限
                )
                // 5.接口放行/鉴权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // 登录、注册所有接口免登录放行
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // 开放Swagger文档
                        .requestMatchers("/api/**").authenticated() // 所有/api下接口必须登录携带Token
                        .anyRequest().permitAll() // 其余全部请求直接放行
                )
                // 6.在账号密码校验过滤器之前执行JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 允许所有前端域名
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许全部常用请求方式
        config.setAllowedHeaders(List.of("*")); // 放行所有请求头（适配前端携带Token的Authorization头）
        config.setAllowCredentials(true); // 允许携带Cookie、凭证

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 全部接口应用跨域规则
        return source;
    }
}