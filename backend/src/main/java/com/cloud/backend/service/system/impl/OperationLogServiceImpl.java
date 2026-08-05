package com.cloud.backend.service.system.impl;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.PageRequest;
import com.cloud.backend.dto.admin.LogFilterRequest;
import com.cloud.backend.dto.admin.LogItem;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.mapper.OperationLogMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现。
 * 写入口 log() 统一受配置中心 system.enable-operation-log 开关控制
 * （手动埋点与 @Log 切面均经此处，关闭时直接跳过，读不受影响）。
 *
 * 修改指引：
 * - 【习惯】想改"操作日志总开关（system.enable-operation-log）" → log() 中 settingsService.isOperationLogEnabled()；
 *   改动影响全站操作日志写入（手动埋点与 @Log 切面共用此入口）
 * - 【习惯】想改"日志查询维度/过滤条件" → listByFilter()/listByFilterPaged() 与 OperationLogMapper.findByFilter/
 *   countByFilter/findPaged 的 SQL；改动影响管理端日志筛选与分页
 * - 【习惯】想改"日志字段写入" → log() 内 operationLog 实体字段与 OperationLogMapper.insert；
 *   改动影响日志完整性（需与 @Log 切面/手动埋点取值对齐）
 * - 【习惯】与接口联动：本类实现 OperationLogService，改签名/行为须同步接口契约及 OperationLogAspect、
 *   AuthServiceImpl/FileServiceImpl/ShareServiceImpl 等埋点调用方
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final AdminSettingsService settingsService;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper, AdminSettingsService settingsService) {
        this.operationLogMapper = operationLogMapper;
        this.settingsService = settingsService;
    }

    @Override
    public void log(OperationLog operationLog) {
        // 操作日志开关（system.enable-operation-log，ADMIN 配置），关闭时不写库
        if (!settingsService.isOperationLogEnabled()) {
            return;
        }
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

    @Override
    public Page<LogItem> listByFilterPaged(LogFilterRequest filter, PageRequest pageRequest) {
        long total = operationLogMapper.countByFilter(filter);
        List<LogItem> records = operationLogMapper.findPaged(filter, pageRequest);
        return new Page<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }
}
