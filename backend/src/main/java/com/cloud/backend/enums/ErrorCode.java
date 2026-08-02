package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 全局错误码枚举 —— 与 DDD.md 2.2 节保持一致。
 *
 * 设计思路：
 * 按 DDD.md 的区域划分：
 * - 200: SUCCESS
 * - 10000-10003, 10500: 通用错误
 * - 10100-10199: 认证授权
 * - 10200-10299: 文件管理
 * - 10300-10399: 分享管理
 * - 10400-10499: 团队空间
 *
 * 前端只需判断 code !== 200 即为失败，根据具体 code 展示对应文案。
 */
@Getter
public enum ErrorCode {

    /* ==================== 通用 ==================== */
    SUCCESS(200, "成功"),
    BAD_REQUEST(10000, "请求参数错误"),
    UNAUTHORIZED(10001, "未登录"),
    FORBIDDEN(10002, "无权限"),
    NOT_FOUND(10003, "资源不存在"),
    INTERNAL_ERROR(10500, "服务器内部错误"),

    /* ==================== 认证 (10100-10199) ==================== */
    LOGIN_LOCKED(10100, "账号已锁定"),
    CAPTCHA_INVALID(10101, "验证码错误"),
    CAPTCHA_COOLDOWN(10102, "验证码发送过于频繁"),
    OLD_PASSWORD_INVALID(10103, "旧密码不匹配"),
    USER_NOT_FOUND(10104, "用户不存在"),
    USER_ALREADY_EXISTS(10105, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(10106, "邮箱已被注册"),
    WRONG_CREDENTIALS(10107, "用户名或密码错误"),
    ACCOUNT_DISABLED(10108, "账号已被禁用"),
    INVALID_TOKEN(10109, "Token 无效"),
    TOKEN_EXPIRED(10110, "Token 已过期"),
    REGISTER_DISABLED(10111, "注册功能未开放"),
    MAIL_NOT_ENABLED(10112, "邮件服务未开启"),

    /* ==================== 文件 (10200-10299) ==================== */
    FILE_NAME_DUPLICATE(10200, "文件名已存在"),
    FILE_QUOTA_EXCEEDED(10201, "空间配额不足"),
    FILE_NOT_FOUND(10202, "文件不存在"),
    UPLOAD_INVALID(10203, "上传参数错误"),
    UPLOAD_CHUNK_MISSING(10204, "分片缺失"),
    UPLOAD_MERGE_FAILED(10205, "分片合并失败"),
    FILE_UPLOAD_FAILED(10206, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(10207, "文件下载失败"),
    UPLOAD_TASK_EXCEEDED(10208, "上传任务数超过限制"),
    FILE_TOO_LARGE(10209, "单文件大小超过限制"),
    UPLOAD_NOT_FOUND(10210, "上传任务不存在或已过期"),
    UPLOAD_ALREADY_MERGED(10211, "上传任务已合并"),
    RECYCLE_NOT_FOUND(10212, "回收站记录不存在"),
    PREVIEW_UNSUPPORTED(10213, "该文件类型暂不支持预览"),
    BATCH_TASK_NOT_FOUND(10214, "打包任务不存在"),
    MOVE_INVALID(10215, "不能移动到自身或其子目录"),
    FILE_DISABLED(10216, "文件已被管理员禁用"),
    UPLOAD_BLOCKED(10217, "上传违规文件"),

    /* ==================== 分享 (10300-10399) ==================== */
    SHARE_EXPIRED(10300, "分享已过期"),
    SHARE_PASSWORD_REQUIRED(10301, "需要提取码"),
    SHARE_PASSWORD_INVALID(10302, "提取码错误"),
    SHARE_NOT_FOUND(10303, "分享不存在"),
    SHARE_EXHAUSTED(10304, "分享下载次数已达上限"),
    SHARE_SAVE_DISABLED(10305, "该分享不允许转存"),
    SHARE_CANCELED(10306, "分享已取消"),
    SHARE_PASSWORD_LOCKED(10307, "提取码错误次数过多，请重新打开链接"),
    SHARE_DOWNLOAD_DISABLED(10308, "该分享禁止下载"),
    SHARE_FILE_REMOVED(10309, "分享的文件已不存在"),
    SHARE_PASSWORD_EMPTY(10310, "请输入提取码"),
    SHARE_COUNT_LIMIT(10311, "该文件分享次数已达上限"),

    /* ==================== 团队 (10400-10499) ==================== */
    TEAM_NAME_DUPLICATE(10400, "团队名已存在"),
    TEAM_NOT_FOUND(10401, "团队不存在"),
    TEAM_MEMBER_EXISTS(10402, "成员已在团队中"),
    TEAM_OWNER_CANNOT_LEAVE(10403, "所有者不能退出团队"),
    TEAM_QUOTA_EXCEEDED(10404, "团队空间配额不足"),
    TEAM_MEMBER_NOT_FOUND(10405, "成员不存在"),
    TEAM_NOT_MEMBER(10406, "你不是团队成员"),
    TEAM_PERMISSION_DENIED(10407, "没有团队操作权限"),
    TEAM_LIMIT_EXCEEDED(10408, "团队数量已达上限"),
    TEAM_MEMBER_LIMIT_EXCEEDED(10409, "团队成员数已达上限"),

    /* ==================== 好友 (10600-10699) ==================== */
    FRIEND_NOT_FOUND(10600, "好友关系不存在"),
    FRIEND_REQUEST_NOT_FOUND(10601, "好友请求不存在"),
    FRIEND_CANNOT_ADD_SELF(10602, "不能添加自己为好友"),
    FRIEND_ALREADY(10603, "你们已经是好友了"),
    FRIEND_REQUEST_PENDING(10604, "好友请求处理中，请勿重复发送"),

    /* ==================== 存储 ==================== */
    MINIO_ERROR(10501, "MinIO 存储异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
