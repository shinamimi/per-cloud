package com.cloud.backend.service.admin;

/**
 * 系统设置服务 —— 上传限制配置项（管理员可配置，VIP 差异化）。
 * t_setting 表有记录时优先，否则使用配置文件默认值。
 */
public interface AdminSettingsService {

    long getMaxSizeUser();

    long getMaxSizeVip();

    int getMaxConcurrentUser();

    int getMaxConcurrentVip();

    /** 更新上传限制（null 字段保持原值） */
    void updateUploadLimits(Long maxSizeUser, Long maxSizeVip, Integer maxConcurrentUser, Integer maxConcurrentVip);
}
