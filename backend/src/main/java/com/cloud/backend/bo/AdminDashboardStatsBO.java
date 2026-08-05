package com.cloud.backend.bo;

/**
 * 管理后台仪表盘统计业务对象 —— 聚合用户数、文件数、总容量与总配额等指标。
 *
 * 设计思路：
 * 1. 使用率（usagePercent）由构造器统一计算，调用方无需重复实现换算
 * 2. 总配额为 0 时使用率按 0 处理（除零保护），避免前端展示异常
 *
 * 修改指引：
 * - 【习惯】新增统计指标            → 新增字段 + 构造器参数；聚合来源需在 DashboardService（管理后台查询）同步补充
 * - 【习惯】修改使用率计算方式      → usagePercent 计算公式在构造器中；当前为 totalSize/totalQuota*100，改动影响管理端仪表盘展示
 * - 【习惯】修改除零保护行为        → 构造器中 totalQuota > 0 判断；当前配额为 0 时使用率按 0 处理
 * - 【习惯】修改字段类型/单位       → 各字段当前均为 long（字节）；改动需同步统计查询 SQL 与前端展示单位
 */
public class AdminDashboardStatsBO {

    /** 用户总数 */
    private long userCount;
    /** 文件总数（含目录） */
    private long fileCount;
    /** 全部文件占用空间总和（单位：字节） */
    private long totalSize;
    /** 全部用户配额总和（单位：字节） */
    private long totalQuota;
    /** 整体配额使用率（百分比，0~100） */
    private double usagePercent;

    /**
     * 构造统计对象并计算使用率。
     * 总配额为 0（未配置任何配额）时使用率按 0 处理。
     */
    public AdminDashboardStatsBO(long userCount, long fileCount, long totalSize, long totalQuota) {
        this.userCount = userCount;
        this.fileCount = fileCount;
        this.totalSize = totalSize;
        this.totalQuota = totalQuota;
        this.usagePercent = totalQuota > 0 ? (double) totalSize / totalQuota * 100 : 0;
    }

    public long getUserCount() { return userCount; }
    public long getFileCount() { return fileCount; }
    public long getTotalSize() { return totalSize; }
    public long getTotalQuota() { return totalQuota; }
    public double getUsagePercent() { return usagePercent; }
}
