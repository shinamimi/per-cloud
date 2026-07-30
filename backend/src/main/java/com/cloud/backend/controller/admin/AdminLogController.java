package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminLogResponse;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Result<List<AdminLogResponse>> listLogs() {
        List<AdminLogResponse> logs = operationLogService.listAll().stream()
                .map(l -> new AdminLogResponse(l.getId(), l.getUserId(), l.getOperation(),
                        l.getTargetType(), l.getTargetId(), l.getDetail(), l.getIp(),
                        l.getCreatedAt()))
                .toList();
        return Result.success(logs);
    }
}
