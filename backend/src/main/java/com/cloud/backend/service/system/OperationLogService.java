package com.cloud.backend.service.system;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.PageRequest;
import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.dto.admin.LogItem;
import com.cloud.backend.entity.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口。
 * 记录用户关键操作（登录、上传、删除等），后台可审计。
 *
 * 修改指引：
 * - 【习惯】想改"写入开关（system.enable-operation-log）" → log() 对应 OperationLogServiceImpl 中
 *   settingsService.isOperationLogEnabled() 关闭时跳过写库（手动埋点与 @Log 切面均经此处，读不受影响）；
 *   改动影响日志采集
 * - 【习惯】想改"查询接口（按用户/按过滤条件/管理端分页 join 用户名）" → listByUserId()/listAll()/listByFilter()/
 *   listByFilterPaged() 对应 OperationLogMapper 的 SQL（operation 传 LOGIN 即登录日志）；
 *   改动影响管理日志页与登录日志
 * - 【习惯】想改"日志保存天数" → getOperationLogDays()/getLoginLogDays()（AdminSettingsService，定时清理依据）；
 *   改动影响日志保留策略
 * - 【习惯】新增方法 → 需同步实现类 OperationLogServiceImpl 与 AdminLogController
 */
public interface OperationLogService {

    void log(OperationLog operationLog);

    List<OperationLog> listByUserId(Long userId);

    List<OperationLog> listAll();

    List<OperationLog> listByFilter(LogFilterRequest filter);

    /** 管理端分页查询（join 用户名；operation 传 LOGIN 即登录日志） */
    Page<LogItem> listByFilterPaged(LogFilterRequest filter, PageRequest pageRequest);
}