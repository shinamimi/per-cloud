package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String storagePath;
    private long chunkSize;
    private long maxSize;
}