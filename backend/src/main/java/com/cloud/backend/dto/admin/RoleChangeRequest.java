package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量变更角色请求项 —— PUT /api/admin/admins/batch 的元素。
 * newRole 取值：OPERATOR / ADMIN（设管理员）、USER（降级），不允许 SUPER_ADMIN。
 *
 * 修改指引：
 * - 【习惯】修改必填校验          → userId/newRole 均标注 @NotNull；改动影响批量变更接口契约与前端表单
 * - 【习惯】修改 newRole          → 自定义枚举 Role（enums/Role.java：USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100）；
 *                           仅允许 OPERATOR/ADMIN/USER（不可 SUPER_ADMIN，由服务层校验），改动需同步校验与前端角色选择
 * - 【习惯】修改字段名/类型        → 对应批量变更接口的请求参数，改动需同步 service 批量逻辑与前端
 */
@Data
public class RoleChangeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "newRole 不能为空")
    private Role newRole;
}
