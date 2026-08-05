package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置映射。
 *
 * 修改指引（yml 前缀 jwt.）：
 * - 【习惯】secret      → jwt.secret；签名密钥；改动后所有已签发 Token 验签失败（全员需重新登录），生产必须用环境变量覆盖强密钥
 * - 【习惯】expiration  → jwt.expiration；单位毫秒，默认 86400000（24 小时）；改动后影响 Token 有效期与过期重登频率
 * - 【习惯】header      → jwt.header；默认 Authorization；改动后需与前端携带 Token 的请求头一致
 * - 【习惯】prefix      → jwt.prefix；默认 Bearer；改动后需与前端 "Bearer xxxxx" 前缀一致
 * - 【习惯】issuer      → jwt.issuer；默认 cloud；改动后影响签发者标识与验签一致性
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