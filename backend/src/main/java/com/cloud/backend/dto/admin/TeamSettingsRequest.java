package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class TeamSettingsRequest {

    private Integer maxPerUser;
    private Long defaultQuota;
    private Integer recycleBinDays;
    private Integer maxMembers;
}
