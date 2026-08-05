package com.cloud.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 老用户配额批量调整请求。
 * 日期范围必选；preview=true 时只返回受影响用户明细，不执行修改（幂等预览）。
 *
 * 修改指引：
 * - 【习惯】修改必填日期范围       → startDate/endDate（LocalDate）标注 @NotNull；改动影响批量调整接口契约与前端日期选择
 * - 【习惯】修改 role/status 过滤  → String 枚举值：role 为 ALL/USER/VIP，status 为 ALL/NORMAL/DISABLED/LOCKED/INACTIVE；
 *                           改动需同步过滤逻辑与前端下拉选项
 * - 【习惯】修改 targetQuotaUser/targetQuotaVip → Long 目标配额（单位字节，普通用户/VIP 各自），标注 @NotNull；
 *                           改动影响批量调整结果，需同步配额更新逻辑
 * - 【习惯】修改 preview 语义      → Boolean 默认 false；true 仅返回受影响用户明细不执行修改（幂等预览），改动需同步 service 分支
 * - 【习惯】修改字段类型/单位      → 目标配额为字节，前端需换算展示；改动需同步配额换算与展示
 */
@Data
public class QuotaBatchRequest {

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** ALL / USER / VIP */
    private String role = "ALL";

    /** ALL / NORMAL / DISABLED / LOCKED / INACTIVE */
    private String status = "ALL";

    @NotNull(message = "普通用户目标配额不能为空")
    private Long targetQuotaUser;

    @NotNull(message = "VIP 目标配额不能为空")
    private Long targetQuotaVip;

    /** true=仅预览（返回受影响用户明细），false=执行批量调整 */
    private Boolean preview = false;

    public boolean isPreview() {
        return Boolean.TRUE.equals(preview);
    }
}
