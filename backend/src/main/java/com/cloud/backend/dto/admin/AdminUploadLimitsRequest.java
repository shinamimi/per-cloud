package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class AdminUploadLimitsRequest {

    private Long maxSizeUser;
    private Long maxSizeVip;
    private Integer maxConcurrentUser;
    private Integer maxConcurrentVip;
}
