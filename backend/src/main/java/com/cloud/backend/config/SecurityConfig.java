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

/**
 * Spring Security 配置 —— 组装 JWT 认证过滤器链、URL 权限矩阵与 CORS。
 *
 * 设计思路：
 * - 无状态会话：不创建会话（STATELESS），认证信息全部来自 JWT 过滤器
 * - 统一异常出口：未认证/权限不足分别交由 EntryPoint 与 AccessDeniedHandler 处理
 * - 权限矩阵按路径前缀收敛：认证白名单（登录、接口文档、访客分享）→ 管理端角色分级 → 其余 /api/** 需登录
 * - CORS 全局放行（允许任意来源与凭证），实际防护依赖 Token 而非 Cookie，故关闭 CSRF
 *
 * 修改指引：
 * - 【习惯】修改放行白名单（免登录路径） → securityFilterChain() 中 .requestMatchers(...).permitAll()；
 *                               改动后影响免认证访问范围，放开需谨慎评估安全风险
 * - 【习惯】修改管理端角色分级        → securityFilterChain() 中 .requestMatchers("/api/admin/**").hasAnyRole(...)；
 *                               改动后影响 ADMIN/OPERATOR/SUPER_ADMIN 的接口权限
 * - 【习惯】修改会话策略             → .sessionManagement(SessionCreationPolicy.STATELESS)；改为有状态需同步 Token 认证机制
 * - 【习惯】修改密码编码算法          → PasswordEncoderConfig.passwordEncoder()；影响注册/登录比对，见该类修改指引
 * - 【习惯】修改 CORS 策略           → corsConfigurationSource() 中 setAllowedOriginPatterns/Methods/Headers/AllowCredentials；
 *                               改动后影响跨域请求是否放行（当前任意来源可跨域）
 * - 【习惯】修改 CSRF 开关           → .csrf(AbstractHttpConfigurer::disable)；开启后需同步前端携带 CSRF Token
 * - 【习惯】修改 JWT 过滤器挂载位置    → .addFilterBefore(...)；改动后影响过滤链顺序
 * - 【习惯】修改未认证/权限不足出口    → .exceptionHandling().authenticationEntryPoint(...)/.accessDeniedHandler(...)，
 *                               对应 AuthenticationEntryPointImpl / AccessDeniedHandlerImpl
 */
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
