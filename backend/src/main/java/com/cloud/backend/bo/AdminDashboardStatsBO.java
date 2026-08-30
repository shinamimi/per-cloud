package com.cloud.backend.bo;

public class AdminDashboardStatsBO {

    /** 用户总数；【统一】改后需同步 DashboardService 聚合来源（管理后台查询） */
    private long userCount;
    /** 文件总数（含目录）；【统一】改后需同步 DashboardService 聚合来源（管理后台查询） */
    private long fileCount;
    /** 全部文件占用空间总和（单位：字节）；【统一】改后需同步 DashboardService 聚合来源（管理后台查询）与统计查询 SQL/前端展示单位 */
    private long totalSize;
    /** 全部用户配额总和（单位：字节）；【统一】改后需同步 DashboardService 聚合来源（管理后台查询）与统计查询 SQL/前端展示单位 */
    private long totalQuota;
    /** 整体配额使用率（百分比，0~100）；【统一】改后需同步 DashboardService 聚合来源（管理后台查询） */
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
