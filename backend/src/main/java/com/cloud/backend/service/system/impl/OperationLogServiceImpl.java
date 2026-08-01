package com.cloud.backend.service.system.impl;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.PageRequest;
import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.dto.admin.LogItem;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.mapper.OperationLogMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现。
 * 写入口 log() 统一受配置中心 system.enable-operation-log 开关控制
 * （手动埋点与 @Log 切面均经此处，关闭时直接跳过，读不受影响）。
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final AdminSettingsService settingsService;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper, AdminSettingsService settingsService) {
        this.operationLogMapper = operationLogMapper;
        this.settingsService = settingsService;
    }

    @Override
    public void log(OperationLog operationLog) {
        // 操作日志开关（system.enable-operation-log，ADMIN 配置），关闭时不写库
        if (!settingsService.isOperationLogEnabled()) {
            return;
        }
        operationLogMapper.insert(operationLog);
    }

    @Override
    public List<OperationLog> listByUserId(Long userId) {
        return operationLogMapper.findByUserId(userId);
    }

    @Override
    public List<OperationLog> listAll() {
        return operationLogMapper.findAll();
    }

    @Override
    public List<OperationLog> listByFilter(LogFilterRequest filter) {
        return operationLogMapper.findByFilter(filter);
    }

    @Override
    public Page<LogItem> listByFilterPaged(LogFilterRequest filter, PageRequest pageRequest) {
        long total = operationLogMapper.countByFilter(filter);
        List<LogItem> records = operationLogMapper.findPaged(filter, pageRequest);
        return new Page<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }
}
