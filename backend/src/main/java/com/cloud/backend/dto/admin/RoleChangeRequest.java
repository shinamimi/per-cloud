package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量变更角色请求项 —— PUT /api/admin/admins/batch 的元素。
 * newRole 取值：OPERATOR / ADMIN（设管理员）、USER（降级），不允许 SUPER_ADMIN。
 */
@Data
public class RoleChangeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "newRole 不能为空")
    private Role newRole;
}
