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
 *
 * 修改指引：
 * - 【习惯】新增业务异常抛出方式     → 直接 new BusinessException(ErrorCode.xxx) 或 (ErrorCode.xxx, "自定义消息")；
 *                             新错误码需先在 enums/ErrorCode.java 定义，否则编译不过
 * - 【习惯】修改异常父类            → extends RuntimeException；改为受检异常会让所有 throw 处强制声明处理
 * - 【习惯】修改携带的字段          → 私有字段 + @Getter；新增字段需同步构造器，并确认 GlobalExceptionHandler 是否需要取用
 * - 【习惯】修改默认错误消息来源    → ErrorCode.getMessage()；调整文案应改 enums/ErrorCode.java，而非本类
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