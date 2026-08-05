package com.cloud.backend.dto.admin;

import com.cloud.backend.entity.RecycleBin;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局回收站记录响应 —— GET /api/admin/files/recycle-bin。
 * 仅管理员删除（deleted_by=1）的记录；teamId=0 为个人空间文件，>0 为团队文件。
 * userName/teamName 由服务层填充（展示归属）。
 *
 * 修改指引：
 * - 【习惯】修改响应字段名/类型    → 字段为前端回收站列表取值依据，改动需同步 AdminRecycleService 的组装（from()）与前端
 * - 【习惯】修改 type             → Integer 回收站记录类型；改动需同步删除/恢复逻辑与前端类型展示
 * - 【习惯】修改 size 单位         → 当前为 Long 字节，前端需换算展示；改动影响回收站容量列展示
 * - 【习惯】修改 userName/teamName → 由服务层填充；改动需同步填充逻辑，否则返回 null
 * - 【习惯】修改 deletedTime/expireTime → LocalDateTime 删除/过期时间，前端用于恢复窗口展示；改动需同步查询 SQL
 */
@Data
public class AdminRecycleResponse {

    private Long id;
    private Long fileId;
    private Long userId;
    private String userName;
    private Long teamId;
    private String teamName;
    private String originalName;
    private Integer type;
    private Long size;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;

    public static AdminRecycleResponse from(RecycleBin record) {
        AdminRecycleResponse response = new AdminRecycleResponse();
        response.setId(record.getId());
        response.setFileId(record.getFileId());
        response.setUserId(record.getUserId());
        response.setTeamId(record.getTeamId());
        response.setOriginalName(record.getOriginalName());
        response.setType(record.getType());
        response.setSize(record.getSize());
        response.setDeletedTime(record.getDeletedTime());
        response.setExpireTime(record.getExpireTime());
        return response;
    }
}
