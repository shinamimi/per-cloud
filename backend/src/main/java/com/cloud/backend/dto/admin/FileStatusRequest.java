package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

/**
 * 管理端文件状态变更请求 —— PUT /api/admin/files/{id}/status。
 * NORMAL=启用，DISABLED=禁用；scope 仅禁用时生效：
 * GLOBAL=全站禁（按内容 hash），USER=仅用户（默认）。
 *
 * 修改指引：
 * - 【统一】修改 status           → 自定义枚举 FileStatus（enums/FileStatus.java：DELETED=0/NORMAL=1/DISABLED=2），存储 TINYINT；
 *                           改后需同步枚举定义与状态变更执行逻辑
 * - 【统一】修改 scope            → 自定义枚举 DisableScope（enums/DisableScope.java：GLOBAL=1 全站禁/USER=2 仅用户），默认 USER；
 *                           GLOBAL 按内容 hash 全站禁用；改后需同步禁用执行逻辑
 * - 【统一】修改默认 scope        → 当前默认 DisableScope.USER；改后需同步前端默认值说明与禁用粒度逻辑
 */
@Data
public class FileStatusRequest {

    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
