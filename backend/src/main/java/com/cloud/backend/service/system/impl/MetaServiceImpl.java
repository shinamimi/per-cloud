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
