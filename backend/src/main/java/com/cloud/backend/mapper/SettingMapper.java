package com.cloud.backend.mapper;

import com.cloud.backend.entity.Setting;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统设置 Mapper —— t_setting 表（key-value）。
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
