package com.cloud.backend.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 老用户配额批量调整结果。
 * 预览时 users 返回受影响用户明细（只读），执行时仅返回 count。
 */
@Data
public class QuotaBatchResponse {

    /** 受影响用户数量 */
    private int count;

    /** 预览明细（preview=true 时非空） */
    private List<AdminUserResponse> users;
}
