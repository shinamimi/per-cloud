package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 配额调整请求 DTO —— 后台设置用户/团队的管理端赠送额度。
 */
@Data
public class QuotaRequest {
    /** 管理端赠送配额（单位：字节） */
    private Long adminBonusQuota;
}