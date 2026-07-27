package com.cloud.backend.service;

import com.cloud.backend.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务。
 *
 * 设计思路：
 * 使用 Spring 的 JavaMailSender 发送 HTML 格式邮件。
 * 目前只用于发送验证码，预留了 sendHtmlMail 方法给其他场景（如通知邮件）。
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public EmailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("邮件发送失败");
        }
    }
}