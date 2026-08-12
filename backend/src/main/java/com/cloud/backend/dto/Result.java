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
 *
 * 修改指引：
 * - 【统一】修改响应字段名        → code/message/data 为全局统一响应格式，改动会影响所有接口与前端拦截器（code===200 判断成功）；改后需同步所有接口与前端拦截器
 * - 【统一】修改成功判定约定      → 当前前端以 code===200 判断成功；改 code 或新增字段会破坏全部接口契约，需同步前端拦截器；改后需同步前端拦截器与全部接口契约
 * - 【统一】修改静态工厂方法      → success/fail 系列方法在各 controller 中调用；改动返回结构需同步所有调用处；改后需同步所有调用处与前端拦截器
 * - 【习惯】新增错误码            → 在 ErrorCode 枚举中新增，并用 fail(ErrorCode) 返回；改动影响错误展示文案与前端处理分支
 * - 【统一】修改 data 泛型        → T 为业务数据；需保持各接口返回结构与前端类型定义一致；改后需同步各接口返回结构与前端类型定义
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
        result.setCode(ErrorCode.INTERNAL_ERROR.getCode());
        result.setMessage(message);
        return result;
    }
}