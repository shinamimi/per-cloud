package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 系统功能开关更新请求（null 字段恢复配置默认值）。仅 ADMIN 可保存。
 *
 * 修改指引：
 * - 【统一】修改 allowRegister     → Boolean 是否允许注册；改动需同步注册接口入口判断与前端注册入口
 * - 【统一】修改 allowGuestShare   → Boolean 是否允许游客分享；改动需同步分享接口的游客鉴权
 * - 【统一】修改 enableMailVerify  → Boolean 是否启用邮箱验证；改动需同步注册/找回密码的验证流程
 * - 【统一】修改 enableCaptcha     → Boolean 是否启用验证码；改动需同步登录/注册验证码校验逻辑
 * - 【统一】修改 enableOperationLog → Boolean 是否启用操作日志；改动需同步 LogAspect 是否记录日志
 * - 【统一】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 */
@Data
public class SystemSettingsRequest {

    private Boolean allowRegister;
    private Boolean allowGuestShare;
    private Boolean enableMailVerify;
    private Boolean enableCaptcha;
    private Boolean enableOperationLog;
}
