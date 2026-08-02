package com.cloud.backend.dto;

import lombok.Data;

/**
 * 管理端全局文件查询条件 —— GET /api/admin/files。
 * userId 可选（按用户 ID 精确筛选）；username 可选（按用户名或昵称模糊筛选）；
 * teamId 可选（0=个人空间，>0=具体团队，-1=全部团队，null=全部）；
 * category 类型过滤；status 可选（1=NORMAL 2=DISABLED，null=全部未删除）；
 * sort：timeDesc（默认）/ sizeDesc / sizeAsc。
 */
@Data
public class AdminFileQuery {

    private Long userId;
    private String username;
    private Long teamId;
    private Integer category;
    private Integer status;
    private String sort;
    private int page = 1;
    private int size = 20;
    /** 分页偏移（由 service 根据 page/size 计算，XML 分页用） */
    private int offset = 0;
}
