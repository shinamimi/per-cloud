package com.cloud.backend.mapper;

import com.cloud.backend.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendshipMapper {

    int insert(Friendship friendship);

    /** 好友列表：user_a_id 或 user_b_id 命中即返回（对端 id 由业务层剥离） */
    List<Friendship> findByUserId(Long userId);

    /** 判断两人是否为好友 */
    Friendship findByPair(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);

    int deleteByPair(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);
}
