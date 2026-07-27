package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储相关配置。
 * storagePath：文件实际落盘路径（当直传时使用）
 * chunkSize：分片上传的默认分片大小
 * maxSize：单文件大小上限
 */
@Data
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String storagePath;
    private long chunkSize;
    private long maxSize;
}