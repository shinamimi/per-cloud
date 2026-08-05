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
 *
 * 修改指引：
 * - 【习惯】写入操作日志          → insert（XML：src/main/resources/mapper/OperationLogMapper.xml）；operation/target_type 以枚举
 *                          name() 字符串存库，改枚举取值需同步 DDL 或兼容老数据
 * - 【习惯】查询日志              → findByUserId / findAll / findByFilter（XML 同上）；findByFilter 为动态条件
 *                          （userId/operation/targetType/时间范围），改过滤字段需同步 XML 与 LogFilterRequest
 * - 【习惯】管理端分页            → findPaged / countByFilter（XML 同上）；findPaged 为 LEFT JOIN t_user 带出用户名的多表查询，
 *                          改 join 或返回列（LogItem）需同步 XML；offset/size 由 PageRequest 计算传入，两方法条件必须一致
 * - 【习惯】清理过期日志          → deleteByCreatedAtBefore（XML 同上）；operation 语义：具体类型 / "NON_LOGIN"=排除 LOGIN / null=全部，
 *                          改清理规则需与定时清理任务的保留天数配置联动
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