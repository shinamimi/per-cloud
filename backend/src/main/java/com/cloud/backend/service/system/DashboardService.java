package com.cloud.backend.service.system;

import com.cloud.backend.bo.AdminDashboardStatsBO;

/**
 * 仪表盘服务接口 —— 提供后台全局统计指标。
 *
 * 修改指引：
 * - 【习惯】想改"统计口径（用户数/文件数/总容量/总配额/使用率）" → getStats() 对应 DashboardServiceImpl 的全表内存
 *   聚合（findAll + stream 求和）；改动影响后台仪表盘数值
 * - 【习惯】想改"性能（全表统计数据量大时的风险）" → getStats() 改用数据库聚合查询（Mapper count/sum）替代内存聚合；
 *   改动影响统计性能与实现依赖
 * - 【习惯】新增方法 → 需同步实现类 DashboardServiceImpl 与 AdminDashboardController
 */
public interface DashboardService {

    /**
     * 统计用户数、文件数、总容量与总配额，并计算整体使用率。
     * 注意：当前实现为全表统计，数据量大时需评估聚合查询替代。
     */
    AdminDashboardStatsBO getStats();
}
