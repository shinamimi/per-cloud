package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 邮件服务配置更新请求（null 字段恢复配置默认值）。仅 ADMIN 可保存。
 * password 为空或等于脱敏占位符时表示不修改 SMTP 密码。
 *
 * 修改指引：
 * - 【统一】修改单位             → frequencyLimit 单位为秒（邮件频率限制）；改动需同步邮件发送限流逻辑与前端
 * - 【统一】修改 encryption       → String：STARTTLS / SSL / NONE；改动需同步邮件客户端初始化逻辑与前端下拉
 * - 【统一】修改 password 语义    → 空/脱敏占位符 = 不更新 SMTP 密码；改动需同步 service 的脱敏判断，否则会误清密码
 * - 【统一】修改 port             → Integer SMTP 端口；改动需同步邮件客户端连接配置
 * - 【统一】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 */
@Data
public class MailSettingsRequest {

    /** SMTP 开关 */
    private Boolean enabled;

    private String host;
    private Integer port;

    /** SMTP 加密方式：STARTTLS / SSL / NONE */
    private String encryption;

    private String username;

    /** SMTP 密码（空/脱敏占位符 = 不更新） */
    private String password;

    /** 发件人邮箱地址 */
    private String from;

    /** 发件人显示名 */
    private String fromName;

    /** 邮件频率限制（秒） */
    private Long frequencyLimit;
}
