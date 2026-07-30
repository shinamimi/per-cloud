package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.OperationLog;
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
    public Result<List<OperationLog>> listLogs() {
        return Result.success(operationLogService.listAll());
    }
}
