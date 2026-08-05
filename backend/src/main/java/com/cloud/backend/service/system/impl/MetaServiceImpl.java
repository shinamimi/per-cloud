package com.cloud.backend.service.system.impl;

import com.cloud.backend.dto.meta.MetaOptionsResponse;
import com.cloud.backend.dto.meta.OptionItem;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.ShareStatus;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.service.system.MetaService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典服务实现 —— 组装 GET /api/meta/options 返回的所有枚举组。
 *
 * 设计思路：
 * - role 组做显示层混淆：OPERATOR→管理员、ADMIN→超级管理员；SUPER_ADMIN 不暴露
 * - 只返回 value + label，颜色/图标/Tag 类型归前端维护
 * - 枚举运行时不变，组装结果可整体缓存（当前直接组装，量小无性能问题）
 *
 * 修改指引：
 * - 【习惯】想改"各枚举组标签（value→label 映射）" → ROLE_LABELS/USER_STATUS_LABELS/SHARE_STATUS_LABELS/
 *   OPERATION_TYPE_LABELS；改动影响前端下拉/筛选的显示文案
 * - 【习惯】想改"role 组混淆与排除（OPERATOR→管理员、ADMIN→超级管理员、SUPER_ADMIN 不暴露）" → roleOptions() 的
 *   ROLE_LABELS 与 filter(role != SUPER_ADMIN)；改动影响管理端角色选择范围与显示
 * - 【习惯】想改"枚举组增删" → getOptions() 中 groups.put(...) 与 toOptions()；新增枚举值时须同步 LABELS，
 *   否则对应项 label 为 null
 * - 【习惯】与枚举联动：本类强依赖 Role/UserStatus/ShareStatus/OperationType 枚举的 name() 作为 key，
 *   改枚举名/新增取值须同步本类 LABELS，否则前端展示缺失
 * - 【习惯】与接口联动：本类实现 MetaService，改签名/行为须同步接口契约及 MetaController 调用方
 */
@Service
public class MetaServiceImpl implements MetaService {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            Role.USER.name(), "普通用户",
            Role.OPERATOR.name(), "管理员",
            Role.ADMIN.name(), "超级管理员");

    private static final Map<String, String> USER_STATUS_LABELS = Map.of(
            UserStatus.DISABLED.name(), "禁用",
            UserStatus.NORMAL.name(), "正常",
            UserStatus.LOCKED.name(), "锁定",
            UserStatus.INACTIVE.name(), "未活跃");

    private static final Map<String, String> SHARE_STATUS_LABELS = Map.of(
            ShareStatus.NORMAL.name(), "有效",
            ShareStatus.EXPIRED.name(), "已过期",
            ShareStatus.CANCELED.name(), "已取消");

    private static final Map<String, String> OPERATION_TYPE_LABELS = Map.ofEntries(
            Map.entry(OperationType.LOGIN.name(), "登录"),
            Map.entry(OperationType.REGISTER.name(), "注册"),
            Map.entry(OperationType.UPLOAD_FILE.name(), "上传文件"),
            Map.entry(OperationType.DOWNLOAD_FILE.name(), "下载文件"),
            Map.entry(OperationType.DELETE_FILE.name(), "删除文件"),
            Map.entry(OperationType.RESTORE_FILE.name(), "恢复文件"),
            Map.entry(OperationType.CREATE_SHARE.name(), "创建分享"),
            Map.entry(OperationType.CANCEL_SHARE.name(), "取消分享"),
            Map.entry(OperationType.UPDATE_USER.name(), "修改用户"),
            Map.entry(OperationType.TEAM_CREATE.name(), "创建团队"),
            Map.entry(OperationType.TEAM_DISSOLVE.name(), "解散团队"),
            Map.entry(OperationType.TEAM_INVITE.name(), "邀请成员"),
            Map.entry(OperationType.TEAM_REMOVE.name(), "移除成员"),
            Map.entry(OperationType.TEAM_LEAVE.name(), "退出团队"),
            Map.entry(OperationType.RESET_PASSWORD.name(), "重置密码"));

    @Override
    public MetaOptionsResponse getOptions() {
        Map<String, List<OptionItem>> groups = new LinkedHashMap<>();
        groups.put("userStatus", toOptions(UserStatus.values(), USER_STATUS_LABELS));
        groups.put("role", roleOptions());
        groups.put("shareStatus", toOptions(ShareStatus.values(), SHARE_STATUS_LABELS));
        groups.put("operationType", toOptions(OperationType.values(), OPERATION_TYPE_LABELS));
        return new MetaOptionsResponse(groups);
    }

    /** role 组：混淆 label，排除 SUPER_ADMIN（不暴露在此页 UI 中） */
    private List<OptionItem> roleOptions() {
        return Arrays.stream(Role.values())
                .filter(role -> role != Role.SUPER_ADMIN)
                .map(role -> new OptionItem(role.name(), ROLE_LABELS.get(role.name())))
                .toList();
    }

    private List<OptionItem> toOptions(Enum<?>[] values, Map<String, String> labels) {
        return Arrays.stream(values)
                .map(e -> new OptionItem(e.name(), labels.get(e.name())))
                .toList();
    }
}
