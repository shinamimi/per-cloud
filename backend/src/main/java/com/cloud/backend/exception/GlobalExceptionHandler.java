package com.cloud.backend.exception;

import com.cloud.backend.dto.Result;
import com.cloud.backend.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    /** 兜底异常处理器 —— 捕获所有未处理异常，返回 500 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR, e.getMessage());
    }
}