package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件服务配置映射，用于发送验证码等邮件。
 */
@Data
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String host;     // SMTP 服务器地址
    private int port;        // SMTP 端口
    private String username; // 发件人邮箱
    private String password; // SMTP 授权码（非登录密码）
    private String from;
}