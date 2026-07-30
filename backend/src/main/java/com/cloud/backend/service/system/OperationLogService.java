package com.cloud.backend.service.system;

import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.entity.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口。
 * 记录用户关键操作（登录、上传、删除等），后台可审计。
 */
public interface OperationLogService {

    void log(OperationLog operationLog);

    List<OperationLog> listByUserId(Long userId);

    List<OperationLog> listAll();

    List<OperationLog> listByFilter(LogFilterRequest filter);
}