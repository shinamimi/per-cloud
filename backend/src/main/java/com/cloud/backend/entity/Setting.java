package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统设置实体 —— 对应数据库 t_setting 表（key-value）。
 * 管理员可配置项（如上传限制）存储于此，无记录时使用配置文件默认值。
 */
@Data
public class Setting {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
