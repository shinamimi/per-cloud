package com.cloud.backend.controller.admin;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.dto.Result;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台仪表盘控制器 —— 提供全局统计指标（用户数、文件数、容量使用率）。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 全局统计指标，供仪表盘首页展示。
     */
    @GetMapping("/stats")
    public Result<AdminDashboardStatsBO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
