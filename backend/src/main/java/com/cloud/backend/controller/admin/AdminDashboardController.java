package com.cloud.backend.controller.admin;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.dto.Result;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台仪表盘控制器 —— 提供全局统计指标（用户数、文件数、容量使用率）。
 *
 * 修改指引：
 * - 【习惯】全局统计           → GET /api/admin/dashboard/stats，调 dashboardService.getStats()；权限 OPERATOR+
 *                        （SecurityConfig /api/admin/** hasAnyRole("OPERATOR","ADMIN","SUPER_ADMIN")），
 *                        改动影响仪表盘指标口径
 * - 【习惯】新增/修改接口       → 在 @RequestMapping("/api/admin/dashboard") 下新增；注意 SecurityConfig 权限级别，
 *                        前端管理端 API 层需同步
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
