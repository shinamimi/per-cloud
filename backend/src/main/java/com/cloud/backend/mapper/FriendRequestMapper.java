package com.cloud.backend.mapper;

import com.cloud.backend.entity.FriendRequest;
import com.cloud.backend.enums.FriendRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRequestMapper {

    int insert(FriendRequest request);

    /** 双方最近一条请求记录（用于幂等判断：不能重复发送待处理请求） */
    FriendRequest findLatestBetween(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /** 某用户收到的待处理请求（对方视角 pending，倒序） */
    List<FriendRequest> findPendingByToUserId(Long toUserId);

    /** 某用户发起的待处理请求（倒序，用于前端展示已发送状态） */
    List<FriendRequest> findPendingByFromUserId(Long fromUserId);

    FriendRequest findById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") FriendRequestStatus status);
}
