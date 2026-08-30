package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /** 【统一】改后需同步 yml mail.host+读取方(EmailService)（无单位，SMTP 服务器地址） */
    private String host;     // SMTP 服务器地址
    /** 【统一】改后需同步 yml mail.port+读取方(EmailService)（无单位，SMTP 端口号） */
    private int port;        // SMTP 端口
    /** 【统一】改后需同步 yml mail.username+读取方(EmailService)（无单位，发件人邮箱） */
    private String username; // 发件人邮箱
    /** 【统一】改后需同步 yml mail.password+读取方(EmailService)（无单位，SMTP 授权码，勿提交明文） */
    private String password; // SMTP 授权码（非登录密码）
    /** 【统一】改后需同步 yml mail.from+读取方(EmailService)（无单位，发件显示地址） */
    private String from;
}