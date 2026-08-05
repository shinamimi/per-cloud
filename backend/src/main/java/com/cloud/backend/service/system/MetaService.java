package com.cloud.backend.service.system;

import com.cloud.backend.dto.meta.MetaOptionsResponse;

/**
 * 字典服务 —— 组装前端管理后台所需的业务枚举选项。
 * 枚举来自 Java Enum，运行时不变，前端登录后拉取一次即可。
 *
 * 修改指引：
 * - 【习惯】想改"返回的枚举组（增删组）" → getOptions() 对应 MetaServiceImpl 的组装逻辑与 Map groups；
 *   改动影响管理后台下拉选项
 * - 【习惯】想改"角色显示混淆（OPERATOR→管理员/ADMIN→超级管理员，SUPER_ADMIN 不暴露）" → roleOptions() 与
 *   ROLE_LABELS；改动影响管理端角色展示与可操作性
 * - 【习惯】想改"某枚举的 label 文案/新增枚举值" → USER_STATUS_LABELS/SHARE_STATUS_LABELS/OPERATION_TYPE_LABELS 等
 *   常量；改动须与对应枚举类（UserStatus/ShareStatus/OperationType）同步
 * - 【习惯】想改"缓存组装结果" → getOptions() 整体结果缓存（当前每次组装，量小无性能问题）；改动影响字典刷新时机
 * - 【习惯】新增方法 → 需同步实现类 MetaServiceImpl 与 MetaController
 */
public interface MetaService {

    MetaOptionsResponse getOptions();
}
