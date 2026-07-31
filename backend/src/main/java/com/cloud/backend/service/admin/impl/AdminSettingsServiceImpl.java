package com.cloud.backend.service.admin.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.entity.Setting;
import com.cloud.backend.mapper.SettingMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.stereotype.Service;

/**
 * 系统设置服务实现。
 * key 定义：upload.max-size-user / upload.max-size-vip / upload.max-concurrent-user / upload.max-concurrent-vip
 */
@Service
public class AdminSettingsServiceImpl implements AdminSettingsService {

    private static final String KEY_MAX_SIZE_USER = "upload.max-size-user";
    private static final String KEY_MAX_SIZE_VIP = "upload.max-size-vip";
    private static final String KEY_MAX_CONCURRENT_USER = "upload.max-concurrent-user";
    private static final String KEY_MAX_CONCURRENT_VIP = "upload.max-concurrent-vip";

    private final SettingMapper settingMapper;
    private final FileProperties fileProperties;

    public AdminSettingsServiceImpl(SettingMapper settingMapper, FileProperties fileProperties) {
        this.settingMapper = settingMapper;
        this.fileProperties = fileProperties;
    }

    @Override
    public long getMaxSizeUser() {
        return readLong(KEY_MAX_SIZE_USER, fileProperties.getMaxSizeUser());
    }

    @Override
    public long getMaxSizeVip() {
        return readLong(KEY_MAX_SIZE_VIP, fileProperties.getMaxSizeVip());
    }

    @Override
    public int getMaxConcurrentUser() {
        return readInt(KEY_MAX_CONCURRENT_USER, fileProperties.getMaxConcurrentUser());
    }

    @Override
    public int getMaxConcurrentVip() {
        return readInt(KEY_MAX_CONCURRENT_VIP, fileProperties.getMaxConcurrentVip());
    }

    @Override
    public void updateUploadLimits(Long maxSizeUser, Long maxSizeVip, Integer maxConcurrentUser, Integer maxConcurrentVip) {
        // null 表示恢复默认值（删除 t_setting 配置行），非 null 表示覆盖
        upsertOrReset(KEY_MAX_SIZE_USER, maxSizeUser, "普通用户单文件大小上限（字节）");
        upsertOrReset(KEY_MAX_SIZE_VIP, maxSizeVip, "VIP 用户单文件大小上限（字节）");
        upsertOrReset(KEY_MAX_CONCURRENT_USER, maxConcurrentUser, "普通用户上传并发任务数上限");
        upsertOrReset(KEY_MAX_CONCURRENT_VIP, maxConcurrentVip, "VIP 用户上传并发任务数上限");
    }

    private void upsertOrReset(String key, Object value, String description) {
        if (value == null) {
            settingMapper.deleteByKey(key);
            return;
        }
        upsert(key, String.valueOf(value), description);
    }

    private long readLong(String key, long defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int readInt(String key, int defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void upsert(String key, String value, String description) {
        Setting setting = new Setting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        settingMapper.upsert(setting);
    }
}
