package com.cloud.backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * MinIO 对象存储配置。
 *
 * 设计思路：
 * 1. 读取 MinioProperties 配置，创建 MinioClient Bean，供 StorageServiceImpl 注入使用
 * 2. 利用 ApplicationReadyEvent 在应用完全启动后（所有 Bean 就绪）自动创建桶
 * 3. auto-create-bucket 通过 @ConditionalOnProperty 控制开关，方便生产环境关闭
 *
 * 为什么不用 @PostConstruct？
 * 因为 MinioClient Bean 刚创建时网络可能未就绪，ApplicationReadyEvent 更稳妥。
 *
 * 修改指引：
 * - 【习惯】修改 MinioClient 构建参数 → minioClient() @Bean；改动后影响全部对象存储读写（端点/凭证）
 * - 【习惯】修改自动建桶开关         → initBucket() 的 @ConditionalOnProperty(prefix="minio", name="auto-create-bucket")；
 *                             生产关闭后启动不再自动建桶，需预先在 MinIO 手动创建
 * - 【习惯】修改建桶时机            → initBucket() 的 @EventListener(ApplicationReadyEvent.class)；改动后影响桶就绪时间点
 * - 【习惯】修改默认桶名            → properties.getBucket()；由 MinioProperties.bucket 提供，改动后影响文件落桶位置
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties properties;

    public MinioConfig(MinioProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * 启动后自动初始化存储桶。
     * 实现原理：先检查桶是否存在，不存在则创建。
     * 避免在业务层每次上传时都判断桶是否存在，减少重复代码。
     */
    @EventListener(ApplicationReadyEvent.class)
    @ConditionalOnProperty(prefix = "minio", name = "auto-create-bucket", havingValue = "true", matchIfMissing = false)
    public void initBucket() {
        try {
            MinioClient client = minioClient();
            String bucket = properties.getBucket();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket [{}] created", bucket);
            } else {
                log.info("MinIO bucket [{}] already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
        }
    }
}