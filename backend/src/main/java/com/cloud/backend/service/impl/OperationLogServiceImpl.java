package com.cloud.backend.service.impl;

import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.mapper.OperationLogMapper;
import com.cloud.backend.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

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
}