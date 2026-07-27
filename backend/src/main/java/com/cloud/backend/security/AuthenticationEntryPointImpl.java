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