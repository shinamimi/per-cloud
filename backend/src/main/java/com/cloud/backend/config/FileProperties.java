package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** 【统一】改后需同步 yml file.storage-path+读取方(FileServiceImpl 注入)（无单位，磁盘路径） */
    private String storagePath;
    /** 【统一】改后需同步 yml file.chunk-size+读取方(UploadServiceImpl)（字节） */
    private long chunkSize;
    /** 【统一】改后需同步 yml file.max-size+读取方(暂无直接读取，AdminSettingsServiceImpl 注入)（字节） */
    private long maxSize;
    /** 【统一】改后需同步 yml file.max-size-user+读取方(AdminSettingsServiceImpl)（字节） */
    private long maxSizeUser;
    /** 【统一】改后需同步 yml file.max-size-vip+读取方(AdminSettingsServiceImpl)（字节） */
    private long maxSizeVip;
    /** 【统一】改后需同步 yml file.max-concurrent-user+读取方(AdminSettingsServiceImpl)（并发任务数） */
    private int maxConcurrentUser;
    /** 【统一】改后需同步 yml file.max-concurrent-vip+读取方(AdminSettingsServiceImpl)（并发任务数） */
    private int maxConcurrentVip;
    /** 【统一】改后需同步 yml file.small-file-threshold+读取方(UploadServiceImpl)（字节） */
    private long smallFileThreshold;
    /** 【统一】改后需同步 yml file.upload-expire-hours+读取方(UploadServiceImpl)（小时） */
    private int uploadExpireHours;
    /** 【统一】改后需同步 yml file.package-expire-hours+读取方(DownloadServiceImpl)（小时） */
    private int packageExpireHours;
    /** 【统一】改后需同步 yml file.recycle-days+读取方(AdminSettingsServiceImpl)（天） */
    private int recycleDays;
    /** 【统一】改后需同步 yml file.preview-text-max-size+读取方(PreviewServiceImpl)（字节） */
    private long previewTextMaxSize;
}
