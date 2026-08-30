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
     * 生成 presigned URL 专用的 client（用 public-url 做 endpoint）。
     * S3 v4 签名把 host 值签进签名（SignedHeaders=host），若用内网 endpoint 签名，
     * 浏览器访问公网地址时签名校验必失败（403）。故 presigned 必须用「浏览器可达」的公网地址签名。
     */
    @Bean
    public MinioClient presignMinioClient() {
        String endpoint = properties.getPublicUrl();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = properties.getEndpoint();
        }
        return MinioClient.builder()
                .endpoint(endpoint)
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