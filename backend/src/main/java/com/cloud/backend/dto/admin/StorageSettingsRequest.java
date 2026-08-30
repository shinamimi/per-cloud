package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class StorageSettingsRequest {

    private Long defaultQuotaUser;
    private Long defaultQuotaVip;
}
