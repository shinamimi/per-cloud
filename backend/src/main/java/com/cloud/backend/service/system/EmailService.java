package com.cloud.backend.service.system;

import com.cloud.backend.config.MailProperties;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.service.admin.AdminSettingsService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> defaultMailSenderProvider;
    private final MailProperties mailProperties;
    private final AdminSettingsService settingsService;

    public EmailService(ObjectProvider<JavaMailSender> defaultMailSenderProvider,
                        MailProperties mailProperties,
                        AdminSettingsService settingsService) {
        this.defaultMailSenderProvider = defaultMailSenderProvider;
        this.mailProperties = mailProperties;
        this.settingsService = settingsService;
    }

    /** 发送验证码邮件，HTML 模板渲染 */
    public void sendCaptchaMail(String to, String code, String purpose) {
        String subject = "Cloud 云盘 - " + purpose;
        String html = """
                <div style="max-width:480px;margin:0 auto;padding:24px;font-family:Arial,sans-serif">
                    <h2>Cloud 云盘</h2>
                    <p>您的验证码为：</p>
                    <div style="font-size:32px;font-weight:bold;letter-spacing:8px;text-align:center;padding:16px;background:#f5f5f5;border-radius:8px;margin:16px 0">
                        %s
                    </div>
                    <p style="color:#999">验证码 5 分钟内有效，请勿泄露给他人。</p>
                </div>
                """.formatted(code);
        sendHtmlMail(to, subject, html);
    }

    public void sendHtmlMail(String to, String subject, String html) {
        // SMTP 开关（mail.enabled，ADMIN 配置）
        if (!settingsService.isMailEnabled()) {
            log.warn("Mail disabled by settings, skip sending to {}", to);
            throw new BusinessException(ErrorCode.MAIL_NOT_ENABLED);
        }
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String fromName = settingsService.getMailFromName();
            if (fromName != null && !fromName.isBlank()) {
                helper.setFrom(defaultFrom(), fromName);
            } else {
                helper.setFrom(defaultFrom());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件发送失败");
        }
    }

    /**
     * 构建邮件发送器，回落链：
     * 1. 配置中心 mail.host（动态构建，保存即生效）
     * 2. yml spring.mail.* 自动配置的 sender（local/dev）
     * 3. yml 顶层 mail.*（prod，MailProperties）
     */
    private JavaMailSender buildMailSender() {
        String host = settingsService.getMailHost();
        if (host != null && !host.isBlank()) {
            return buildSender(host, settingsService.getMailPort(),
                    settingsService.getMailEncryption(),
                    settingsService.getMailUsername(), settingsService.getMailPassword());
        }
        JavaMailSender auto = defaultMailSenderProvider.getIfAvailable();
        if (auto != null) {
            return auto;
        }
        if (mailProperties.getHost() != null && !mailProperties.getHost().isBlank()) {
            return buildSender(mailProperties.getHost(), mailProperties.getPort(),
                    "STARTTLS",
                    mailProperties.getUsername(), mailProperties.getPassword());
        }
        throw new BusinessException(ErrorCode.MAIL_NOT_ENABLED, "邮件服务未配置");
    }

    private JavaMailSender buildSender(String host, int port, String encryption,
                                       String username, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        String enc = encryption == null ? "" : encryption.trim().toUpperCase();
        switch (enc) {
            case "SSL" -> {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.starttls.required", "false");
            }
            case "NONE" -> {
                props.put("mail.smtp.ssl.enable", "false");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.starttls.required", "false");
            }
            default -> { // STARTTLS
                props.put("mail.smtp.ssl.enable", "false");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
        }
        return sender;
    }

    /** 发件人地址：配置中心 mail.from（含 yml spring.mail.from 兜底）→ 顶层 mail.from（prod）→ 兜底 */
    private String defaultFrom() {
        String configured = settingsService.getMailFrom();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank()) {
            return mailProperties.getFrom();
        }
        return "noreply@cloud.local";
    }
}
