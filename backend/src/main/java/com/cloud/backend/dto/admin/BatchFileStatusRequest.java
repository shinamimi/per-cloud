package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

import java.util.List;

/**
 * 管理端批量状态变更请求 —— POST /api/admin/files/batch-status。
 * scope 仅禁用时生效（GLOBAL=全站禁 / USER=仅用户，默认 USER）。
 *
 * 修改指引：
 * - 【统一】修改 ids              → List<Long> 批量文件 ID；改后需同步批量变更 SQL 的 IN 条件与前端多选逻辑
 * - 【统一】修改 status           → 自定义枚举 FileStatus（enums/FileStatus.java：DELETED=0/NORMAL=1/DISABLED=2），存储 TINYINT；
 *                           改后需同步枚举定义与批量变更执行逻辑
 * - 【统一】修改 scope            → 自定义枚举 DisableScope（enums/DisableScope.java：GLOBAL=1 全站禁/USER=2 仅用户），默认 USER；
 *                           仅禁用时生效；改后需同步禁用执行逻辑（按 hash 全站禁或按用户禁）
 * - 【统一】修改默认 scope        → 当前默认 DisableScope.USER；改后需同步前端默认值说明与禁用粒度逻辑
 */
@Data
public class BatchFileStatusRequest {

    private List<Long> ids;
    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
