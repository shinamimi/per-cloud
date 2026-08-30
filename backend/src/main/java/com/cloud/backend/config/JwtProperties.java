package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 【统一】改后需同步 yml jwt.secret+读取方(JwtTokenUtilImpl)（无单位，Base64 编码字符串） */
    private String secret;      // 签名密钥，用于 HMAC 算法签名和验签
    /** 【统一】改后需同步 yml jwt.expiration+读取方(AdminSettingsServiceImpl)（毫秒） */
    private long expiration;    // Token 过期时间（毫秒）
    /** 【统一】改后需同步 yml jwt.header+读取方(JwtAuthenticationFilter 硬编码"Authorization"未直接读取)（无单位，HTTP 请求头名） */
    private String header;      // 请求头名称，默认 Authorization
    /** 【统一】改后需同步 yml jwt.prefix+读取方(JwtAuthenticationFilter 硬编码"Bearer "未直接读取)（无单位，Token 前缀字符串） */
    private String prefix;      // Token 前缀，默认 Bearer
    /** 【统一】改后需同步 yml jwt.issuer+读取方(JwtTokenUtilImpl)（无单位，签发者标识字符串） */
    private String issuer;      // 签发者标识
}