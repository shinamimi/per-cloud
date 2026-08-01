package com.cloud.backend.mapper;

import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Mapper。
 */
@Mapper
public interface OperationLogMapper {

    int insert(OperationLog operationLog);

    List<OperationLog> findByUserId(Long userId);

    List<OperationLog> findAll();

    List<OperationLog> findByFilter(LogFilterRequest filter);

    /** 按创建时间批量删除（日志保留天数清理任务用） */
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff,
                                @Param("operation") String operation);
}