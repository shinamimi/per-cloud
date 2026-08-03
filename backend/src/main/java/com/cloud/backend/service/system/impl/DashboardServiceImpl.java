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
