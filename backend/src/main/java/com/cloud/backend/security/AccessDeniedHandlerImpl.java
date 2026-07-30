package com.cloud.backend.security;

import com.cloud.backend.enums.ErrorCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已登录但权限不足的全局处理器。
 *
 * 设计思路：
 * 与 AuthenticationEntryPointImpl 的区别：
 * - AuthenticationEntryPointImpl：未登录（未提供 Token 或 Token 无效）
 * - AccessDeniedHandlerImpl：已登录但角色不符（如普通用户访问 /api/admin/**）
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"code\":" + ErrorCodeEnum.FORBIDDEN.getCode()
                + ",\"message\":\"" + ErrorCodeEnum.FORBIDDEN.getMessage() + "\",\"data\":null}");
    }
}