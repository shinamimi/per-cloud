package com.cloud.backend.mapper;

import com.cloud.backend.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友关系 Mapper —— t_friendship 表。
 * 好友成对存储（user_a_id < user_b_id，由 Service 层归一化），查询命中唯一索引。
 *
 * 设计思路：
 * 1. 成对存储避免重复好友关系，findByPair 按归一化后的 user_a < user_b 查询
 * 2. 唯一索引 uk_pair(user_a_id, user_b_id) 保证两人最多一条好友关系
 *
 * 修改指引：
 * - 【习惯】建立好友关系         → insert（XML：src/main/resources/mapper/FriendshipMapper.xml）；入参需由 Service 按
 *                          user_a < user_b 归一化后写入，user_a_id、user_b_id 参与唯一索引 uk_pair(user_a_id, user_b_id)，
 *                          改字段名需同步数据库 DDL
 * - 【习惯】查询好友列表         → findByUserId（XML 同上）；user_a_id 或 user_b_id 命中任一端即返回，对端 id 由 Service 层剥离，
 *                          改查询条件需同步 XML 与索引（idx_a / idx_b）
 * - 【习惯】判断是否好友         → findByPair（XML 同上）；依赖归一化后的唯一索引，返回 null 表示非好友，
 *                          改匹配规则需同步 Service 层
 * - 【习惯】解除好友关系         → deleteByPair（XML 同上）；按归一化后的 user_a/user_b 删除，改删除条件需同步 XML 与 DDL
 */
@Mapper
public interface FriendshipMapper {

    int insert(Friendship friendship);

    /** 好友列表：user_a_id 或 user_b_id 命中即返回（对端 id 由业务层剥离） */
    List<Friendship> findByUserId(Long userId);

    /** 判断两人是否为好友 */
    Friendship findByPair(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);

    int deleteByPair(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);
}
