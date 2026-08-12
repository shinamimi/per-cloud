package com.cloud.backend.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求 DTO —— 携带新密码，按密码策略校验。
 *
 * 修改指引：
 * - 【统一】修改 newPassword 校验  → @Size(min=9) + @Pattern（必须同时包含数字和英文）；当前策略与注册密码规则不同（此处 9 位起），
 *                           改动影响管理端重置密码接口契约与前端提示；改后需同步注册/找回密码规则与前端提示
 * - 【统一】修改字段名/类型        → 对应 PUT /api/admin/users/{id}/reset-password 的请求参数；改后需同步 service 与前端
 */
@Data
public class AdminResetPasswordRequest {

    /** 新密码：长度大于 8 位，且必须同时包含数字和英文 */
    @Size(min = 9, message = "密码长度必须大于 8 位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码必须同时包含数字和英文")
    private String newPassword;
}
