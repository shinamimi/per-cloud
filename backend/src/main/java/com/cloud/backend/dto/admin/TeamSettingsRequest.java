package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 团队默认值配置。
 * null 字段表示恢复配置文件默认值（删除 t_setting 配置行）。
 *
 * 修改指引：
 * - 【习惯】修改 defaultQuota      → Long 团队默认配额（单位字节）；改动影响新建团队的初始配额
 * - 【习惯】修改 maxPerUser        → Integer 每人可创建团队数上限；改动影响创建团队接口校验
 * - 【习惯】修改 recycleBinDays    → Integer 团队回收站保留天数（天）；改动影响团队回收站清理逻辑
 * - 【习惯】修改 maxMembers        → Integer 团队最大成员数；改动影响团队邀请/加入校验
 * - 【习惯】修改 null 语义         → null 恢复配置默认值（删除 t_setting 配置行）；改动需同步 service 的配置读写逻辑
 */
@Data
public class TeamSettingsRequest {

    private Integer maxPerUser;
    private Long defaultQuota;
    private Integer recycleBinDays;
    private Integer maxMembers;
}
