package com.cloud.backend.exception;

import com.cloud.backend.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常 —— 带 ErrorCode 的 RuntimeException。
 *
 * 设计思路：
 * 在 Service/Controller 层直接 throw BusinessException(ErrorCode.xxx)，利用
 * GlobalExceptionHandler 自动转换为标准 Result 响应，避免每处手动构造 Result.fail()。
 *
 * 两个构造方式：
 * - BusinessException(ErrorCode errorCode) —— 使用 ErrorCode 默认描述
 * - BusinessException(ErrorCode errorCode, String message) —— 自定义描述（不覆盖 ErrorCode 的 code）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}