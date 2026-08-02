package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 修改分享有效期请求 —— PUT /api/shares/{id}。
 */
@Data
public class ShareUpdateRequest {

    /** PERMANENT=永久 / DAYS=按天数（从当前时刻起算） */
    private String validType;

    /** validType=DAYS 时有效期天数（上限 share.max-valid-days） */
    private Integer validDays;
}
