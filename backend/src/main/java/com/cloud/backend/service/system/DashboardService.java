package com.cloud.backend.service.system;

import com.cloud.backend.bo.AdminDashboardStatsBO;

public interface DashboardService {

    /**
     * 统计用户数、文件数、总容量与总配额，并计算整体使用率。
     * 注意：当前实现为全表统计，数据量大时需评估聚合查询替代。
     */
    AdminDashboardStatsBO getStats();
}
