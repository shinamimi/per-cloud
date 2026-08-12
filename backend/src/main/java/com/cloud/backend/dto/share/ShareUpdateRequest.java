package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 修改分享有效期请求 —— PUT /api/shares/{id}。
 *
 * 修改指引：
 * - 【统一】修改 validType       → String validType；PERMANENT=永久 / DAYS=按天数（从当前时刻起算）；
 *                         改动取值需与前端下拉选项及服务端有效期计算保持一致；改后需同步前端下拉选项与服务端有效期计算
 * - 【统一】修改 validDays       → Integer validDays；validType=DAYS 时有效期天数，上限 share.max-valid-days
 *                         （管理员配置），超出服务端 400；该接口仅改有效期，不影响提取码/下载策略等；改后需同步前端表单与服务端有效期计算（含 share.max-valid-days 上限校验）
 */
@Data
public class ShareUpdateRequest {

    /** PERMANENT=永久 / DAYS=按天数（从当前时刻起算） */
    private String validType;

    /** validType=DAYS 时有效期天数（上限 share.max-valid-days） */
    private Integer validDays;
}
