package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 邮件服务配置更新请求（null 字段恢复配置默认值）。仅 ADMIN 可保存。
 * password 为空或等于脱敏占位符时表示不修改 SMTP 密码。
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
