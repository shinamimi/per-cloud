package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统设置实体 —— 对应数据库 t_setting 表（key-value）。
 * 管理员可配置项（如上传限制）存储于此，无记录时使用配置文件默认值。
 *
 * 修改指引：
 * - 【习惯】修改 id                 → Long id；对应 t_setting.id 主键，无业务联动
 * - 【统一】修改 settingKey         → String settingKey；对应 t_setting.setting_key，有唯一约束，业务通过 key 读取配置，
 *                            改 key 需同步 Service 读取处与 DDL；
 *                            改后需同步 DDL 唯一约束与 Service 读取配置处
 * - 【统一】修改 settingValue       → String settingValue；对应 t_setting.setting_value（VARCHAR(255)），数值型配置
 *                            （如配额、上传限制）以字符串存取需自行解析，注意单位（如字节）；无记录时回退配置文件默认值；
 *                            改后需同步所有读取解析方与配置文件默认值的单位口径
 * - 【习惯】修改 description        → String description；仅管理后台展示说明，无业务联动
 * - 【习惯】修改 updatedAt          → LocalDateTime updatedAt；自动维护，无业务联动
 */
@Data
public class Setting {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
