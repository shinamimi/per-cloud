package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友关系实体 —— t_friendship 表。
 * 成对存储（user_a_id < user_b_id），查询便捷、唯一约束防重复。
 *
 * 修改指引：
 * - 【习惯】修改 id                 → Long id；对应 t_friendship.id 主键，无业务联动
 * - 【统一】修改 userAId / userBId  → Long userAId（t_friendship.user_a_id 较小 ID）/ Long userBId（user_b_id 较大 ID）；
 *                            必须保证 user_a_id < user_b_id，唯一索引 uk_pair(user_a_id, user_b_id) 防重复好友，
 *                            改字段名或存储规则需同步 DDL 与好友 Service 的写入顺序；
 *                            改后需同步 uk_pair 唯一索引 DDL、好友 Service 写入顺序与查重逻辑
 * - 【习惯】修改 createdAt          → LocalDateTime createdAt；自动维护，无业务联动
 */
@Data
public class Friendship {

    private Long id;
    private Long userAId;
    private Long userBId;
    private LocalDateTime createdAt;
}
