package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.TeamStatus;
import java.time.LocalDateTime;

/**
 * 后台团队列表响应 DTO —— 团队基本信息 + 配额使用情况 + 成员数。
 *
 * 修改指引：
 * - 【习惯】修改响应字段名/类型    → 字段为前端后台团队列表取值依据，改动需同步团队查询 SQL 与前端
 * - 【习惯】修改 status           → 自定义枚举 TeamStatus（enums/TeamStatus.java：DISSOLVED=0 已解散/NORMAL=1 正常），存储 TINYINT；
 *                           改动需同步枚举定义与前端状态展示
 * - 【习惯】修改 quota/usedSpace   → 单位字节（quota 含基础与赠送部分），前端需换算展示；改动影响团队容量展示，需同步团队配额逻辑
 * - 【习惯】修改 memberCount      → long 成员数量；改动需同步成员统计 SQL
 * - 【习惯】新增响应字段          → 新增字段并同步团队查询 SQL 与前端，否则该字段恒为默认值
 */
public class AdminTeamResponse {

    /** 团队 ID */
    private Long id;
    /** 团队名称 */
    private String name;
    /** 创建者（队长）用户 ID */
    private Long ownerId;
    /** 团队描述 */
    private String description;
    /** 团队状态（NORMAL=正常 / DISSOLVED=已解散） */
    private TeamStatus status;
    /** 团队配额（单位：字节，含基础与赠送部分） */
    private Long quota;
    /** 已用空间（单位：字节） */
    private Long usedSpace;
    /** 成员数量 */
    private long memberCount;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public AdminTeamResponse(Long id, String name, Long ownerId, String description,
                             TeamStatus status, Long quota, Long usedSpace,
                             long memberCount, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.description = description;
        this.status = status;
        this.quota = quota;
        this.usedSpace = usedSpace;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public String getDescription() { return description; }
    public TeamStatus getStatus() { return status; }
    public Long getQuota() { return quota; }
    public Long getUsedSpace() { return usedSpace; }
    public long getMemberCount() { return memberCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
