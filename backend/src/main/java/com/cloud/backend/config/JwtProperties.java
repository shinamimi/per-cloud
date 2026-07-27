package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置映射。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;      // 签名密钥，用于 HMAC 算法签名和验签
    private long expiration;    // Token 过期时间（毫秒）
    private String header;      // 请求头名称，默认 Authorization
    private String prefix;      // Token 前缀，默认 Bearer
    private String issuer;      // 签发者标识
}