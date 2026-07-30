package com.cloud.backend.service.system.impl;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.User;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final FileMapper fileMapper;

    public DashboardServiceImpl(UserMapper userMapper, FileMapper fileMapper) {
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
    }

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
