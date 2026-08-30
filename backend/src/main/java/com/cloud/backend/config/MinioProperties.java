package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** 【统一】改后需同步 yml minio.endpoint+读取方(MinioConfig)（无单位，HTTP URL） */
    private String endpoint;        // MinIO 服务地址，如 http://localhost:9000
    /** 【统一】改后需同步 yml minio.access-key+读取方(MinioConfig)（无单位，访问密钥 ID） */
    private String accessKey;       // 访问密钥 ID
    /** 【统一】改后需同步 yml minio.secret-key+读取方(MinioConfig)（无单位，访问密钥密码，勿提交明文） */
    private String secretKey;       // 访问密钥密码
    /** 【统一】改后需同步 yml minio.bucket+读取方(MinioConfig 与 StorageServiceImpl)（无单位，桶名称） */
    private String bucket;          // 默认存储桶名称
    /** 【统一】改后需同步 yml minio.auto-create-bucket+读取方(MinioConfig @ConditionalOnProperty)（无单位，布尔开关） */
    private boolean autoCreateBucket; // 启动时是否自动创建桶
    /** 【统一】改后需同步 yml minio.public-url+读取方(MinioConfig 构造 presignMinioClient)（无单位，HTTP URL） */
    private String publicUrl;       // 公开访问 URL（用于生成文件访问链接，须为浏览器可达地址）
}