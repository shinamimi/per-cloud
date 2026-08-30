package com.cloud.backend.dto;

import java.util.List;

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
