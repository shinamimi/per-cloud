package com.cloud.backend.security;

import com.cloud.backend.enums.ErrorCodeEnum;
import com.cloud.backend.service.system.JwtBlacklistService;
import com.cloud.backend.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 登录校验过滤器——Spring Security 的入口守卫。
 *
 * 设计思路：
 * 1. 继承 OncePerRequestFilter，保证每个请求只执行一次过滤
 * 2. 从 Authorization 头取 Token，先校验签名和过期时间，再查黑名单
 * 3. 全部通过后从 Token 提取用户名，查数据库构建完整的 Security 上下文
 * 4. 校验失败时不抛异常，而是直接返回 JSON——因为 Spring Security 的异常链在过滤器中处理更优雅，
 *    直接返回结构体统一的错误响应，避免前端再额外处理
 *
 * 工作流程：
 * 请求 → 取出 Header → 验签 + 黑名单 → 查用户 → 设置 SecurityContext → 放行
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                   UserDetailsServiceImpl userDetailsService,
                                   JwtBlacklistService jwtBlacklistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtTokenUtil.validateToken(token)) {
            handleInvalidToken(response, ErrorCodeEnum.TOKEN_EXPIRED.getCode(), "Token 已过期或无效");
            return;
        }

        if (jwtBlacklistService.isBlacklisted(token)) {
            handleInvalidToken(response, ErrorCodeEnum.UNAUTHORIZED.getCode(), "Token 已注销");
            return;
        }

        // 构建 SecurityContext —— 后续请求中可通过 SecurityContextHolder.getContext().getAuthentication() 取到当前用户
        String username = jwtTokenUtil.getUsernameFromToken(token);
        LoginUser loginUser = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void handleInvalidToken(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}