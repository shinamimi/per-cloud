package com.cloud.backend.mapper;

import com.cloud.backend.dto.PageRequest;
import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.dto.admin.LogItem;
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

    /** 分页查询（join 用户表带出用户名），page.offset 由 PageRequest 计算 */
    List<LogItem> findPaged(@Param("filter") LogFilterRequest filter,
                            @Param("page") PageRequest page);

    /** 分页查询总数（与 findPaged 同条件） */
    long countByFilter(@Param("filter") LogFilterRequest filter);

    /** 按创建时间批量删除（日志保留天数清理任务用） */
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff,
                                @Param("operation") String operation);
}