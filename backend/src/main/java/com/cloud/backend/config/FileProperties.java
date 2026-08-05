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
 *
 * 修改指引（yml 前缀 file.，多为字节/时长型配置，注意单位陷阱）：
 * - 【习惯】storage-path         → file.storage-path；默认 /tmp/cloud-storage；改动后影响直传文件落盘位置（注意目录权限与磁盘）
 * - 【习惯】chunk-size           → file.chunk-size；单位字节；默认 10485760（10MB）；改动后影响分片数量与上传性能
 * - 【习惯】max-size             → file.max-size；单位字节；默认 536870912（512MB）；保留兼容，改动后影响旧逻辑的上传上限
 * - 【习惯】max-size-user        → file.max-size-user；单位字节；默认 536870912（512MB）；普通用户单文件上限，改动后影响上传校验与前端提示
 * - 【习惯】max-size-vip         → file.max-size-vip；单位字节；默认 2147483648（2GB）；VIP 用户单文件上限
 * - 【习惯】max-concurrent-user  → file.max-concurrent-user；并发任务数；默认 3；改动后影响普通用户同时上传任务上限
 * - 【习惯】max-concurrent-vip   → file.max-concurrent-vip；并发任务数；默认 5；改动后影响 VIP 用户同时上传任务上限
 * - 【习惯】small-file-threshold → file.small-file-threshold；单位字节；默认 5242880（5MB）；小于该值不分片直传，改动后影响上传路径选择
 * - 【习惯】upload-expire-hours  → file.upload-expire-hours；单位小时；默认 24；改动后影响上传临时分片/元数据 TTL 与 04:00 清理
 * - 【习惯】package-expire-hours → file.package-expire-hours；单位小时；默认 24；改动后影响打包下载产物过期与清理
 * - 【习惯】recycle-days         → file.recycle-days；单位天；默认 30；改动后影响回收站到期物理清理周期（FileCleanupTask 03:00）
 * - 【习惯】preview-text-max-size→ file.preview-text-max-size；单位字节；默认 1048576（1MB）；改动后影响文本预览的最大文件大小
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
