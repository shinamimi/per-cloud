package com.cloud.backend.exception;

import com.cloud.backend.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 业务异常 —— 带 ErrorCodeEnum 的 RuntimeException。
 *
 * 设计思路：
 * 在 Service/Controller 层直接 throw BusinessException(ErrorCodeEnum.xxx)，利用
 * GlobalExceptionHandler 自动转换为标准 Result 响应，避免每处手动构造 Result.fail()。
 *
 * 两个构造方式：
 * - BusinessException(ErrorCodeEnum errorCode) —— 使用 ErrorCodeEnum 默认描述
 * - BusinessException(ErrorCodeEnum errorCode, String message) —— 自定义描述（不覆盖 ErrorCodeEnum 的 code）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCodeEnum errorCode;

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}