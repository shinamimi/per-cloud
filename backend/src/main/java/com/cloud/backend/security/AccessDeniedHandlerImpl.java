package com.cloud.backend.security;

import com.cloud.backend.enums.ErrorCode;
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
 *
 * 修改指引：
 * - 【习惯】修改返回的 HTTP 状态码    → handle() 中 response.setStatus()；目前固定 200 + code 字段，改动需同步前端错误码分支
 * - 【习惯】修改错误码/提示文案       → ErrorCode.FORBIDDEN；改动后影响已登录但权限不足时的统一响应内容
 * - 【习惯】修改返回体结构           → response.getWriter().write() 的 JSON 拼装；需与前端约定保持一致
 * - 【习惯】修改哪些请求会走进这里     → SecurityConfig 的 requestMatchers 权限矩阵（角色/路径）；改动后影响被拒请求的范围
 * - 【习惯】换成其他处理器           → SecurityConfig 的 exceptionHandling().accessDeniedHandler(...)；改动后影响权限不足的统一出口
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"code\":" + ErrorCode.FORBIDDEN.getCode()
                + ",\"message\":\"" + ErrorCode.FORBIDDEN.getMessage() + "\",\"data\":null}");
    }
}