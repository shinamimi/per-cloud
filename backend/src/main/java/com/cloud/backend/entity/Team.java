package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Team {

    /** 团队 ID */
    private Long id;
    /** 团队名称 */
    private String name;
    /** 创建者（队长）用户 ID */
    private Long ownerId;
    /** 团队头像地址 */
    private String avatar;
    /** 团队描述 */
    private String description;
    /** 团队状态（NORMAL=正常 / DISSOLVED=已解散） */
    private TeamStatus status;
    /** 团队总配额（单位：字节） */
    private Long quota;
    /** 已用空间（单位：字节） */
    private Long usedSpace;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最近更新时间 */
    private LocalDateTime updatedAt;
}
