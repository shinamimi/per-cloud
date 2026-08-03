package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友关系实体 —— t_friendship 表。
 * 成对存储（user_a_id < user_b_id），查询便捷、唯一约束防重复。
 */
@Data
public class Friendship {

    private Long id;
    private Long userAId;
    private Long userBId;
    private LocalDateTime createdAt;
}
