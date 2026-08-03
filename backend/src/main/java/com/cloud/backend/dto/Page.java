package com.cloud.backend.dto;

import java.util.List;

/**
 * 通用分页结果包装 —— 统一各列表接口的分页响应结构。
 *
 * 设计思路：
 * 1. 记录列表 + 总条数分离，总条数用于前端分页器计算总页数
 * 2. 分页参数回显（page/size），便于前端续页请求
 */
public class Page<T> {

    /** 当前页记录列表 */
    private List<T> records;
    /** 符合条件的总记录数（非当前页条数） */
    private long total;
    /** 当前页码（从 1 开始） */
    private int page;
    /** 每页条数 */
    private int size;

    public Page(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
