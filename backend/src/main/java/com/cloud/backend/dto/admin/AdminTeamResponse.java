package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.TeamStatus;
import java.time.LocalDateTime;

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
