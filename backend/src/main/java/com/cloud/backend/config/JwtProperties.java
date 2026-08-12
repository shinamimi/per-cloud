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