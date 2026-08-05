package com.cloud.backend.dto;

/**
 * 文件搜索条件。
 * userId 必填；parentId 可选（限定目录）；teamId 为 0/不传表示个人空间，>0 为团队空间；
 * keyword 文件名模糊匹配；category 类型过滤（0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他）；
 * isDirectory 是否只查目录；offset/size 分页。
 *
 * 修改指引：
 * - 【习惯】修改筛选字段名        → userId（必填）、parentId（限定目录）、teamId（0/不传=个人空间，>0=团队空间）、
 *                           keyword（文件名模糊）、mimeTypePrefix（MIME 前缀）、isDirectory（只查目录）、
 *                           category（类型过滤）；字段为文件搜索接口入参，改动需同步查询 SQL 与前端搜索条件
 * - 【习惯】修改 category 取值    → 0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他（对应 FileConstants 分类编号），改动需同步分类映射与前端
 * - 【习惯】修改分页语义          → offset（从 0 起）、size（每页条数，默认 20）；SQL 按 LIMIT offset,size 分页，改动影响列表分页
 * - 【习惯】新增筛选字段          → 新增字段并同步查询 SQL（XML）补充过滤条件，否则该条件不生效
 */
public class FileQuery {

    private Long userId;
    private Long parentId;
    private Long teamId;
    private String keyword;
    private String mimeTypePrefix;
    private Boolean isDirectory;
    private Integer category;
    private int offset = 0;
    private int size = 20;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getMimeTypePrefix() { return mimeTypePrefix; }
    public void setMimeTypePrefix(String mimeTypePrefix) { this.mimeTypePrefix = mimeTypePrefix; }

    public Boolean getIsDirectory() { return isDirectory; }
    public void setIsDirectory(Boolean isDirectory) { this.isDirectory = isDirectory; }

    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
