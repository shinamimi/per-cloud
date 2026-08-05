package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 配额调整请求 DTO —— 后台设置用户/团队的管理端赠送额度。
 *
 * 修改指引：
 * - 【习惯】修改 adminBonusQuota 字段名/类型 → Long 管理端赠送配额（单位字节）；对应配额调整接口请求参数，改动需同步
 *                                       AdminService 配额更新逻辑与前端表单
 * - 【习惯】修改单位             → 字节，前端需换算展示；改动需同步配额换算与展示
 * - 【习惯】新增配额字段          → 新增字段并同步配额更新逻辑与前端，否则该参数不生效
 */
@Data
public class QuotaRequest {
    /** 管理端赠送配额（单位：字节） */
    private Long adminBonusQuota;
}