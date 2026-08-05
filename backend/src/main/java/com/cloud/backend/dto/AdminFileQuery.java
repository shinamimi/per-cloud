package com.cloud.backend.dto;

import lombok.Data;

/**
 * 管理端全局文件查询条件 —— GET /api/admin/files。
 * userId 可选（按用户 ID 精确筛选）；username 可选（按用户名或昵称模糊筛选）；
 * teamId 可选（0=个人空间，>0=具体团队，-1=全部团队，null=全部）；
 * category 类型过滤；status 可选（1=NORMAL 2=DISABLED，null=全部未删除）；
 * sort：timeDesc（默认）/ sizeDesc / sizeAsc。
 *
 * 修改指引：
 * - 【习惯】修改筛选字段名        → userId（Long 精确）、username（用户名/昵称模糊）、teamId（0=个人空间/>0=具体团队/-1=全部团队/null=全部）、
 *                           category（文件类型分类）、status（1=NORMAL 2=DISABLED）；字段对应 GET /api/admin/files 的查询参数，
 *                           改动需同步查询 SQL 与前端筛选条件
 * - 【习惯】修改排序选项          → sort 字符串：timeDesc（默认）/ sizeDesc / sizeAsc；改动需同步 service 排序分支与前端排序下拉
 * - 【习惯】修改分页语义          → page（页码从 1 起，默认 1）、size（每页条数，默认 20）；改动影响列表分页行为与前端分页器
 * - 【习惯】修改 offset           → 由 service 根据 page/size 计算（XML 分页用），改动需同步分页查询逻辑
 * - 【习惯】修改 status/category 取值 → 对应 FileStatus（1=NORMAL/2=DISABLED）与文件分类编号（t_file.category），改动需同步枚举与存量数据
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
