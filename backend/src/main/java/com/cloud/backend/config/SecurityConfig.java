package com.cloud.backend.config;

import com.cloud.backend.security.AccessDeniedHandlerImpl;
import com.cloud.backend.security.AuthenticationEntryPointImpl;
import com.cloud.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

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

    /**
     * 组装安全过滤器链：关闭 CSRF、启用 CORS、配置无状态会话与异常出口，
     * 声明 URL 权限矩阵，并把 JWT 过滤器挂到用户名密码过滤器之前。
     *
     * 权限矩阵要点：
     * - 登录、接口文档、访客分享访问路径公开（转存等写操作在服务内部再校验登录）
     * - 管理端按角色分级：敏感设置与文件管控仅 ADMIN+，其余管理接口 OPERATOR+ 可访问
     * - 其余 /api/** 一律要求认证，未匹配路径（如静态资源）放行
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
                        .requestMatchers("/api/admin/admins/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 敏感配置分组（SMTP 连接信息、系统功能开关）仅 ADMIN+ 可改
                        .requestMatchers("/api/admin/settings/system", "/api/admin/settings/mail")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 文件管控（全局文件/全局回收站）仅 ADMIN+
                        .requestMatchers("/api/admin/files/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "ADMIN", "SUPER_ADMIN")
                        // 访客分享访问公开，转存等写操作在服务内部再校验登录
                        .requestMatchers("/api/shares/access/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 配置：允许任意来源、常见方法与全部请求头，并允许携带凭证。
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
