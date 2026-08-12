package com.cloud.backend.exception;

import com.cloud.backend.dto.Result;
import com.cloud.backend.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 —— 统一所有异常返回格式为 Result<T>。
 *
 * 设计思路：
 * 1. 业务异常（BusinessException）→ 返回自定义错误码和消息
 * 2. 参数校验失败（@Valid）→ 提取字段名和错误描述，拼接为可读消息
 * 3. 未知异常 → 兜底返回 500，暴露异常 message（生产环境按需隐藏）
 *
 * 使用 @RestControllerAdvice 而非 @ControllerAdvice + @ResponseBody，
 * 即一次配置、全局生效。
 *
 * 修改指引：
 * - 【习惯】新增异常类型捕获        → 新增 @ExceptionHandler(XxxException.class) 方法；
 *                             注意与现有处理器的匹配粒度（子类异常会被更具体的处理器拦截），避免重复兜底
 * - 【统一】修改业务异常返回格式    → handleBusinessException；改动影响所有业务异常返回给前端的 code/message；
 *                             改后需同步前端契约（code/message 语义）与 Result 结构
 * - 【习惯】修改参数校验消息拼接    → handleMethodArgumentNotValidException / handleConstraintViolationException；
 *                             改动影响 @Valid 与参数约束校验失败时返回的文案格式
 * - 【习惯】修改兜底 500 行为       → handleException；当前暴露 e.getMessage()，生产环境建议改为固定文案并仅服务端记录完整堆栈
 * - 【统一】新增通用响应字段        → 需同步修改 Result<T> 结构与各 handler 的组装逻辑；
 *                             改后需同步 Result<T> 结构、各 handler 组装逻辑与前端解析
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getErrorCode(), e.getMessage());
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /** 处理 @Valid 校验失败 —— 返回如 "password: 密码长度8-20位, email: 邮箱格式不正确" */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    /** 处理请求体无法反序列化（JSON 格式错误、枚举值非法等）—— 属于客户端错误，返回 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Request body not readable: {}", e.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    /** 上传分片超过 multipart 限制 —— 单分片大小不应超过服务端 chunk-size 配置 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return Result.fail(ErrorCode.FILE_TOO_LARGE, "上传分片大小超过限制");
    }

    /** 兜底异常处理器 —— 捕获所有未处理异常，返回 500 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR, e.getMessage());
    }
}