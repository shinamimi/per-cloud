package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class QuotaRequest {
    /** 管理端赠送配额（单位：字节） */
    private Long adminBonusQuota;
}