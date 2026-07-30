package com.cloud.backend.controller.admin;

import com.cloud.backend.bo.AdminDashboardStatsBO;
import com.cloud.backend.dto.Result;
import com.cloud.backend.service.system.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Result<AdminDashboardStatsBO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
