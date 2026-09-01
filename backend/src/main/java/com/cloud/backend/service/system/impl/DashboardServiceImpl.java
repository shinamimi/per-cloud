package com.cloud.backend.service.system.impl;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
import java.util.List;

=======
>>>>>>> main
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
