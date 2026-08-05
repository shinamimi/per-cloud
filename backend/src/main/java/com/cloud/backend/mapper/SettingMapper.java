package com.cloud.backend.mapper;

import com.cloud.backend.entity.Setting;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统设置 Mapper —— t_setting 表（key-value）。
 *
 * 修改指引：
 * - 【习惯】写入/更新配置        → insert / upsert（XML：src/main/resources/mapper/SettingMapper.xml）；
 *                          upsert 依赖 setting_key 唯一约束（MySQL ON DUPLICATE KEY UPDATE），
 *                          改 key 字段名需同步数据库 DDL，否则 upsert 退化为重复插入
 * - 【习惯】查询配置             → findByKey / findAll（XML 同上）；findByKey 单查返回单条，
 *                          改返回值语义需同步 XML 与 SettingService 的缓存
 * - 【习惯】删除配置             → deleteByKey（XML 同上）；按 setting_key 删除，改删除条件需同步 XML 与 DDL
 */
@Mapper
public interface SettingMapper {

    int insert(Setting setting);

    Setting findByKey(String settingKey);

    List<Setting> findAll();

    /** upsert：已存在则更新值，否则插入 */
    int upsert(Setting setting);

    int deleteByKey(String settingKey);
}
