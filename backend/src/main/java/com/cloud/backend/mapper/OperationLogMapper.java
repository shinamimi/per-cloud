package com.cloud.backend.mapper;

import com.cloud.backend.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    int insert(OperationLog operationLog);

    List<OperationLog> findByUserId(Long userId);
}