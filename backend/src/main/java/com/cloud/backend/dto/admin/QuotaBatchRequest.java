package com.cloud.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 老用户配额批量调整请求。
 * 日期范围必选；preview=true 时只返回受影响用户明细，不执行修改（幂等预览）。
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
