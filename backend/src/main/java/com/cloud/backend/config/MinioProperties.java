package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置映射。
 *
 * 设计思路：通过 @ConfigurationProperties 自动绑定 yml 中以 "minio" 为前缀的配置，
 * 比分散的 @Value 注入更整洁，且支持 IDE 自动提示。
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;        // MinIO 服务地址，如 http://localhost:9000
    private String accessKey;       // 访问密钥 ID
    private String secretKey;       // 访问密钥密码
    private String bucket;          // 默认存储桶名称
    private boolean autoCreateBucket; // 启动时是否自动创建桶
    private String publicUrl;       // 公开访问 URL（用于生成文件访问链接）
}