package com.cloud.backend.dto;

/**
 * 文件搜索条件。
 * userId 必填；parentId 可选（限定目录）；teamId 为 0/不传表示个人空间，>0 为团队空间；
 * keyword 文件名模糊匹配；category 类型过滤（0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他）；
 * isDirectory 是否只查目录；offset/size 分页。
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
