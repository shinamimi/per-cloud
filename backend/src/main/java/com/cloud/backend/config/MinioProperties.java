package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置映射。
 *
 * 设计思路：通过 @ConfigurationProperties 自动绑定 yml 中以 "minio" 为前缀的配置，
 * 比分散的 @Value 注入更整洁，且支持 IDE 自动提示。
 *
 * 修改指引（yml 前缀 minio.）：
 * - 【习惯】endpoint          → minio.endpoint；服务地址，默认 http://localhost:9000；改动后影响客户端连接目标
 * - 【习惯】access-key        → minio.access-key；访问密钥 ID；与 secret-key 配套，改动后影响认证
 * - 【习惯】secret-key        → minio.secret-key；访问密钥密码；勿提交明文
 * - 【习惯】bucket            → minio.bucket；默认 cloud-storage；改动后影响文件所在桶（换桶需考虑存量数据迁移）
 * - 【习惯】auto-create-bucket → minio.auto-create-bucket；默认 dev=true / prod=false；改动后影响启动是否自动建桶
 * - 【习惯】public-url        → minio.public-url；公开访问 URL；改动后影响生成的分享/下载链接地址
 */
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
    /** 【统一】改后需同步 yml minio.public-url+读取方(暂无直接读取，MinioConfig 注入)（无单位，HTTP URL） */
    private String publicUrl;       // 公开访问 URL（用于生成文件访问链接）
}