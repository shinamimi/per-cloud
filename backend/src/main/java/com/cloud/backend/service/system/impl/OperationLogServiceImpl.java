package com.cloud.backend.service.system.impl;

import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.mapper.OperationLogMapper;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现。
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void log(OperationLog operationLog) {
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
}