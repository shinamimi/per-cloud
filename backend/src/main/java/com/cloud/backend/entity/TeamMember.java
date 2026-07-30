package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamMember {

    private Long id;
    private Long teamId;
    private Long userId;
    private TeamMemberRole role;
    private Integer status;
    private LocalDateTime joinedAt;
}
