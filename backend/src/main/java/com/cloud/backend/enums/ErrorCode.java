package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 全局错误码枚举 —— 统一所有 API 响应的 code 和 message。
 *
 * 设计思路：
 * 分区规划错误码：
 * - 2xx/4xx/5xx：HTTP 语义映射
 * - 100xx：用户相关
 * - 200xx：文件相关
 * - 300xx：分享相关
 * - 400xx：认证授权
 * - 500xx：存储层
 *
 * 前端只需要关注 code 字段即可展示对应的国际化文案或弹窗。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "请求资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_ALREADY_EXISTS(10002, "用户已存在"),
    WRONG_PASSWORD(10003, "密码错误"),

    FILE_NOT_FOUND(20001, "文件不存在"),
    FILE_UPLOAD_FAILED(20002, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(20003, "文件下载失败"),

    SHARE_NOT_FOUND(30001, "分享不存在或已过期"),
    SHARE_EXPIRED(30002, "分享已过期"),

    INVALID_TOKEN(40001, "Token 无效"),
    TOKEN_EXPIRED(40002, "Token 已过期"),

    MINIO_ERROR(50001, "MinIO 存储异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}