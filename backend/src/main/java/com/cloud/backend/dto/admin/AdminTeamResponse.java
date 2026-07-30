package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.TeamStatus;
import java.time.LocalDateTime;

public class AdminTeamResponse {

    private Long id;
    private String name;
    private Long ownerId;
    private String description;
    private TeamStatus status;
    private Long quota;
    private Long usedSpace;
    private long memberCount;
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
