package com.cloud.backend.dto;

import java.util.List;

/**
 * 通用分页结果包装 —— 统一各列表接口的分页响应结构。
 *
 * 设计思路：
 * 1. 记录列表 + 总条数分离，总条数用于前端分页器计算总页数
 * 2. 分页参数回显（page/size），便于前端续页请求
 *
 * 修改指引：
 * - 【统一】修改响应字段名        → records/total/page/size 为列表接口统一分页响应结构，改动影响所有使用 Page 的列表接口与前端分页器；改后需同步所有使用 Page 的列表接口与前端分页器
 * - 【统一】修改分页语义          → page 从 1 开始，size 为每页条数；改起始页码会破坏前端分页器对齐，需同步所有分页接口与前端；改后需同步所有分页接口与前端分页器
 * - 【统一】修改 total 含义       → 当前为符合条件的总记录数（非当前页条数），用于计算总页数；改动影响前端分页器显示；改后需同步所有分页接口与前端分页器
 * - 【习惯】新增响应字段          → 新增字段并同步所有调用处与前端，否则该字段恒为默认值
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
