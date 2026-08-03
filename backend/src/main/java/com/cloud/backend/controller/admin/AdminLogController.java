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
