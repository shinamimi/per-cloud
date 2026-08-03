package com.cloud.backend.dto;

/**
 * 通用分页请求参数 —— 页码从 1 开始，未指定时使用默认值。
 *
 * 设计思路：
 * 1. 默认页码 1、每页 20 条，避免前端漏传导致全表查询
 * 2. 提供 getOffset() 统一换算 SQL LIMIT 偏移量，调用方不必重复计算
 */
public class PageRequest {

    /** 当前页码（从 1 开始，默认 1） */
    private int page = 1;
    /** 每页条数（默认 20） */
    private int size = 20;

    public PageRequest() {}

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }

    /**
     * 计算 SQL 分页偏移量（page-1）* size；page 为 1 时偏移为 0。
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
