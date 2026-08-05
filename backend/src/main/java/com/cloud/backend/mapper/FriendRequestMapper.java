package com.cloud.backend.mapper;

import com.cloud.backend.entity.FriendRequest;
import com.cloud.backend.enums.FriendRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友请求 Mapper —— t_friend_request 表。
 * 双向确认制好友（ADR-010）：A 发起请求 → B 接受/拒绝，接受后由 Service 建立 t_friendship 好友关系。
 *
 * 设计思路：
 * 1. status 存字符串枚举（PENDING/ACCEPTED/REJECTED），非 TINYINT，与多数表枚举存储方式不同
 * 2. 幂等依赖 findLatestBetween 查双向最近一条记录，避免重复发送待处理请求
 * 3. 唯一索引 uk_from_to(from_user_id, to_user_id) 保证同一发起方向不重复
 *
 * 修改指引：
 * - 【习惯】发起/查询请求        → insert / findLatestBetween（XML：src/main/resources/mapper/FriendRequestMapper.xml）；
 *                          from_user_id、to_user_id 参与唯一索引 uk_from_to(from_user_id, to_user_id)，改字段名需同步数据库 DDL；
 *                          findLatestBetween 为双向 OR 查询，改匹配方向需同步 XML
 * - 【习惯】查询待处理请求        → findPendingByToUserId / findPendingByFromUserId（XML 同上）；status='PENDING' 且按时间倒序，
 *                          改状态值需与 FriendRequestStatus 枚举（PENDING/ACCEPTED/REJECTED）及 Service 层联动
 * - 【习惯】处理请求             → updateStatus（XML 同上）；仅改 status，接受后由 FriendService 创建好友关系（t_friendship），
 *                          改处理流程需同步 Service 层；status 为 VARCHAR 字符串，改存储方式需同步 DDL
 */
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
