package com.cloud.backend.mapper;

import com.cloud.backend.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作日志 Mapper。
 */
@Mapper
public interface OperationLogMapper {

    int insert(OperationLog operationLog);

    List<OperationLog> findByUserId(Long userId);

    List<OperationLog> findAll();
}