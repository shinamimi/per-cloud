package com.cloud.backend.dto;

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
