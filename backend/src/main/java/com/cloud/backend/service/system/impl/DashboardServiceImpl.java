package com.cloud.backend.service.system.impl;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.User;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仪表盘服务实现 —— 通过全表统计计算后台全局指标。
 *
 * 设计思路：
 * 1. 直接聚合用户表与文件表：用户数、文件数、文件总大小、用户基础配额总和
 * 2. 使用率在统计对象构造时统一计算（总配额为 0 时按 0 处理）
 * 3. 当前为全表内存聚合，数据量增大后应替换为数据库聚合查询（TODO）
 *
 * 修改指引：
 * - 【习惯】想改"统计口径（用户数/文件数/总大小/总配额）" → getStats() 对 UserMapper/FileMapper.findAll() 的聚合；
 *   改动影响后台仪表盘展示
 * - 【习惯】想改"统计实现（全表内存聚合 → 数据库聚合 SQL）" → getStats() 与 AdminDashboardStatsBO 构造；
 *   TODO 注明，改动影响大数据量下的性能（当前全表加载有性能风险）
 * - 【习惯】想改"配额口径" → totalQuota 按 User::getQuota（基础配额）求和；若改为三来源口径（quota+adminBonus+reward）
 *   须同步 AdminDashboardStatsBO 与前端展示
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
     * 统计后台仪表盘指标。
     * 注意：全表加载到内存聚合，用户/文件量大时存在性能风险，需改用聚合 SQL。
     */
    @Override
    public AdminDashboardStatsBO getStats() {
        List<User> users = userMapper.findAll();
        List<File> files = fileMapper.findAll();

        long userCount = users.size();
        long fileCount = files.size();
        long totalSize = files.stream().mapToLong(File::getSize).sum();
        long totalQuota = users.stream().mapToLong(User::getQuota).sum();

        return new AdminDashboardStatsBO(userCount, fileCount, totalSize, totalQuota);
    }
}
