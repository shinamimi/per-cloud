package com.cloud.backend.service.system.impl;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务实现 —— 通过数据库聚合查询计算后台全局指标。
 *
 * 设计思路：
 * 1. 使用 COUNT/SUM 聚合查询替代全表内存聚合，大数据量下性能更优
 * 2. 用户数、文件数、文件总大小、用户基础配额总和均通过 SQL 聚合
 * 3. 使用率在统计对象构造时统一计算（总配额为 0 时按 0 处理）
 *
 * 修改指引：
 * - 【习惯】想改"统计口径（用户数/文件数/总大小/总配额）" → getStats() 对 UserMapper/FileMapper 聚合查询；
 *   改动影响后台仪表盘展示
 * - 【习惯】想改"配额口径" → totalQuota 按 UserMapper.sumQuota()（基础配额）求和；若改为三来源口径
 *   （quota+adminBonus+reward）须同步 AdminDashboardStatsBO 与前端展示
 * - 【习惯】与接口联动：本类实现 DashboardService，改签名/行为须同步接口契约及 AdminDashboardController 调用方
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final FileMapper fileMapper;

    public DashboardServiceImpl(UserMapper userMapper, FileMapper fileMapper) {
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
    }

    /**
     * 统计后台仪表盘指标（使用数据库聚合查询，替代全表内存聚合）。
     */
    @Override
    public AdminDashboardStatsBO getStats() {
        long userCount = userMapper.countAll();
        long fileCount = fileMapper.countAll();
        long totalSize = fileMapper.sumSize();
        long totalQuota = userMapper.sumQuota();

        return new AdminDashboardStatsBO(userCount, fileCount, totalSize, totalQuota);
    }
}
