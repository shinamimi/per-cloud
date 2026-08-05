package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminLogResponse;
import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台操作日志控制器 —— 按用户、操作类型、目标类型、时间范围查询操作日志，供审计排查。
 *
 * 修改指引：
 * - 【习惯】日志查询           → GET /api/admin/logs，调 operationLogService.listByFilter；筛选条件均可选
 *                        （LogFilterRequest 绑定查询参数，不传则查询全部）；权限 OPERATOR+
 *                        （SecurityConfig /api/admin/**），改动影响审计查询能力
 * - 【习惯】新增/修改接口       → 在 @RequestMapping("/api/admin/logs") 下新增；注意 SecurityConfig 权限级别，
 *                        前端管理端 API 层需同步
 */
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 按筛选条件查询操作日志列表，条件均可选（不传则查询全部）。
     */
    @GetMapping
    public Result<List<AdminLogResponse>> listLogs(LogFilterRequest filter) {
        List<AdminLogResponse> logs = operationLogService.listByFilter(filter).stream()
                .map(l -> new AdminLogResponse(l.getId(), l.getUserId(), l.getOperation(),
                        l.getTargetType(), l.getTargetId(), l.getDetail(), l.getIp(),
                        l.getCreatedAt()))
                .toList();
        return Result.success(logs);
    }
}
