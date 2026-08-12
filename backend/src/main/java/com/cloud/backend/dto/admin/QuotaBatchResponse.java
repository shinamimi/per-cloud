package com.cloud.backend.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 老用户配额批量调整结果。
 * 预览时 users 返回受影响用户明细（只读），执行时仅返回 count。
 *
 * 修改指引：
 * - 【统一】修改响应字段名/类型    → count 受影响用户数量、users 预览明细（List<AdminUserResponse>）；字段为前端预览/执行结果取值依据，
 *                           改动需同步批量调整 service 组装逻辑与前端
 * - 【统一】修改 preview 分支语义  → 预览时 users 非空、执行时仅 count；改动需同步 service 的分支返回逻辑
 * - 【统一】修改 users 引用类型    → 依赖 AdminUserResponse 结构；改动该 DTO 字段会影响本响应展示
 */
@Data
public class QuotaBatchResponse {

    /** 受影响用户数量 */
    private int count;

    /** 预览明细（preview=true 时非空） */
    private List<AdminUserResponse> users;
}
