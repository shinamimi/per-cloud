package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储相关配置。
 * storagePath：文件实际落盘路径（当直传时使用）
 * chunkSize：分片上传的默认分片大小
 * maxSize：单文件大小上限（保留兼容）
 * maxSizeUser/maxSizeVip：单文件大小上限（管理员可配置，VIP 差异化，无配置时用默认值）
 * maxConcurrentUser/maxConcurrentVip：上传并发任务数上限（VIP 差异化）
 * smallFileThreshold：小于该阈值不分片（自适应分片）
 * uploadExpireHours：上传临时数据（分片/元数据）过期时间
 * packageExpireHours：打包下载产物过期时间
 * recycleDays：回收站保留天数（到期物理清理）
 * previewTextMaxSize：文本预览的最大大小
 */
@Data
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String storagePath;
    private long chunkSize;
    private long maxSize;
    private long maxSizeUser;
    private long maxSizeVip;
    private int maxConcurrentUser;
    private int maxConcurrentVip;
    private long smallFileThreshold;
    private int uploadExpireHours;
    private int packageExpireHours;
    private int recycleDays;
    private long previewTextMaxSize;
}
