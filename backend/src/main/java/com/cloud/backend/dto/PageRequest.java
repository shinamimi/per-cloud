package com.cloud.backend.dto;

/**
 * 通用分页请求参数 —— 页码从 1 开始，未指定时使用默认值。
 *
 * 设计思路：
 * 1. 默认页码 1、每页 20 条，避免前端漏传导致全表查询
 * 2. 提供 getOffset() 统一换算 SQL LIMIT 偏移量，调用方不必重复计算
 *
 * 修改指引：
 * - 【统一】修改字段名/类型       → page/size 为各分页接口入参，改动需同步所有继承/使用处与前端分页参数；改后需同步所有继承/使用处与前端分页参数
 * - 【习惯】修改默认分页值        → page 默认 1、size 默认 20；改动影响未显式传参的接口返回条数
 * - 【统一】修改分页语义          → page 从 1 起（page=1 时 offset=0），改动破坏与前端分页器约定，需同步所有调用处；改后需同步所有调用处与前端分页器
 * - 【习惯】修改 pageSize 上限    → 当前未设上限；如需防超大 size 需在此新增校验，否则影响数据库查询压力
 * - 【统一】修改 offset 计算      → getOffset() = (page-1)*size；改动影响所有依赖它的 SQL 分页查询；改后需同步所有依赖 getOffset() 的 SQL 分页查询
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
