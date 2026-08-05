package com.cloud.backend.security;

import com.cloud.backend.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录 / Token 无效的全局入口守卫。
 *
 * 设计思路：
 * 当用户未提供有效 Token 时访问受保护的接口，Spring Security 会调用此类的 commence 方法。
 * 这里不跳转登录页（RESTful API 无页面），而是直接返回 JSON 格式的统一错误响应。
 *
 * 修改指引：
 * - 【习惯】修改返回的 HTTP 状态码    → commence() 中 response.setStatus()；目前固定 200 + code 字段，改动需同步前端错误码分支
 * - 【习惯】修改错误码/提示文案       → ErrorCode.UNAUTHORIZED；改动后影响未登录/Token 无效时的统一响应内容
 * - 【习惯】修改返回体结构           → response.getWriter().write() 的 JSON 拼装；需与前端约定保持一致
 * - 【习惯】修改哪些请求属于白名单     → SecurityConfig 中 permitAll 放行路径；改动后影响无需登录即可访问的接口范围
 * - 【习惯】换成其他入口守卫          → SecurityConfig 的 exceptionHandling().authenticationEntryPoint(...)；改动后影响未认证的统一出口
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"code\":" + ErrorCode.UNAUTHORIZED.getCode()
                + ",\"message\":\"" + ErrorCode.UNAUTHORIZED.getMessage() + "\",\"data\":null}");
    }
}