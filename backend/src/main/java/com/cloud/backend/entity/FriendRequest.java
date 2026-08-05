package com.cloud.backend.entity;

import com.cloud.backend.enums.FriendRequestStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友请求实体 —— t_friend_request 表（双向确认关系层）。
 *
 * 设计思路：
 * 好友关系需双方确认：A 发起请求（from_user_id → to_user_id），B 接受后写入 t_friendship，拒绝则结束（可重发）。
 * status 存枚举名称（PENDING/ACCEPTED/REJECTED），由默认 EnumTypeHandler 映射为 VARCHAR。
 *
 * 修改指引：
 * - 【习惯】修改 id                 → Long id；对应 t_friend_request.id 主键，无业务联动
 * - 【习惯】修改 fromUserId / toUserId → Long fromUserId（t_friend_request.from_user_id 发起方）/ Long toUserId（to_user_id 接收方）；
 *                            唯一索引 uk_from_to(from_user_id, to_user_id) 约束同一对请求唯一，改字段名需同步 DDL
 * - 【习惯】修改 status             → FriendRequestStatus status；对应 t_friend_request.status（VARCHAR 存枚举名称），
 *                            PENDING=待接受 / ACCEPTED=已接受 / REJECTED=已拒绝（见 enums/FriendRequestStatus.java）；
 *                            状态机流转（接受写 t_friendship、拒绝可重发）在好友 Service 层，改枚举见 FriendRequestStatus 修改指引
 * - 【习惯】修改 createdAt          → LocalDateTime createdAt；自动维护，无业务联动
 */
@Data
public class FriendRequest {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
}
