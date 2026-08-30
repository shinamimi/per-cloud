package com.cloud.backend.mapper;

import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.enums.TeamMemberRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamMemberMapper {

    int insert(TeamMember teamMember);

    List<TeamMember> findByTeamId(Long teamId);

    long countByTeamId(Long teamId);

    /** 某用户所有团队成员身份（status 过滤：1-正常） */
    List<TeamMember> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /** 某用户在某团队中的身份记录 */
    TeamMember findByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /** 更新成员角色 */
    int updateRole(@Param("id") Long id, @Param("role") TeamMemberRole role);

    /** 退出/移除成员：status 置 0 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 用户所在正常团队数（创建团队数上限校验） */
    long countTeamsByUserId(Long userId);
}
