package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 团队默认值配置（docs/team-module.md §八）。
 * null 字段表示恢复配置文件默认值（删除 t_setting 配置行）。
 */
@Data
public class TeamSettingsRequest {

    private Integer maxPerUser;
    private Long defaultQuota;
    private Integer recycleBinDays;
    private Integer maxMembers;
}
