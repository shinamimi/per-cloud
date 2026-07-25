package com.cloud.backend.service;

import com.cloud.backend.entity.OperationLog;

import java.util.List;

public interface OperationLogService {

    void log(OperationLog operationLog);

    List<OperationLog> listByUserId(Long userId);
}