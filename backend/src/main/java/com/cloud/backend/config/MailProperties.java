package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件服务配置映射，用于发送验证码等邮件。
 *
 * 修改指引（yml 前缀 mail.）：
 * - 【习惯】host     → mail.host；SMTP 服务器地址；改动后影响全部邮件发送
 * - 【习惯】port     → mail.port；SMTP 端口，需与 host 配套（SSL/非 SSL）；改动后影响能否连接 SMTP 服务
 * - 【习惯】username → mail.username；发件人邮箱；改动后影响发件方身份
 * - 【习惯】password → mail.password；SMTP 授权码（非登录密码）；改动后影响 SMTP 认证，勿提交明文到仓库
 * - 【习惯】from     → mail.from；发件显示地址；改动后影响收件方看到的发件人
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