package com.cloud.backend.dto;

import com.cloud.backend.enums.ErrorCode;
import lombok.Data;

/**
 * 全局统一响应体 —— 所有 API 接口的返回值包装为此格式。
 *
 * 设计思路：
 * 前后端约定固定格式 {code, message, data}，前端根据 code 判断成功/失败：
 * - code === 200 → 成功，取 data
 * - code !== 200 → 失败，展示 message
 * 不再依赖 HTTP Status Code 作为业务成功/失败的判断依据，更加灵活。
 *
 * 静态工厂方法：
 * - success(data) —— 带数据成功
 * - success() —— 无数据成功
 * - fail(errorCode) —— 用 ErrorCode 描述失败
 * - fail(errorCode, message) —— 自定义失败描述
 * - fail(message) —— 快速失败（code=500）
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(errorCode.getMessage());
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> result = fail(errorCode);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        result.setMessage(message);
        return result;
    }
}