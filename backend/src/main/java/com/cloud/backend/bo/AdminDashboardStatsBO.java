package com.cloud.backend.bo;

public class AdminDashboardStatsBO {

    private long userCount;
    private long fileCount;
    private long totalSize;
    private long totalQuota;
    private double usagePercent;

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
