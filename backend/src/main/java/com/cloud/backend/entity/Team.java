package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Team {

    private Long id;
    private String name;
    private Long ownerId;
    private String avatar;
    private String description;
    private TeamStatusEnum status;
    private Long quota;
    private Long usedSpace;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
