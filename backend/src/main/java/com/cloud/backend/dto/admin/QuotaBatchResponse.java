package com.cloud.backend.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class QuotaBatchResponse {

    /** 受影响用户数量 */
    private int count;

    /** 预览明细（preview=true 时非空） */
    private List<AdminUserResponse> users;
}
