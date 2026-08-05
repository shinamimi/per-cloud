package com.cloud.backend.mapper;

import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.enums.TeamMemberRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 团队成员 Mapper —— t_team_member 表。
 * 描述用户与团队的成员关系（角色 + 状态），team + user 维度唯一。
 *
 * 设计思路：
 * 1. 唯一索引 uk_team_user(team_id, user_id) 保证同团队同一用户仅一条成员记录
 * 2. status 逻辑维护成员关系有效性（1=正常 0=退出/被移除），记录保留便于追溯
 * 3. 角色用自定义枚举 TeamMemberRole（MEMBER=0/ADMIN=10/OWNER=20），存储 TINYINT
 *
 * 修改指引：
 * - 【习惯】加入成员             → insert（XML：src/main/resources/mapper/TeamMemberMapper.xml）；team_id、user_id 参与唯一索引
 *                          uk_team_user(team_id, user_id)，改字段名需同步数据库 DDL，重复加入会唯一键冲突
 * - 【习惯】查询成员             → findByTeamId / countByTeamId / findByUserIdAndStatus / findByTeamIdAndUserId（XML 同上）；
 *                          默认过滤 status=1 的正常成员，改状态条件需与 TeamMemberService 的退出/移除联动
 * - 【习惯】修改角色/退出成员     → updateRole / updateStatus（XML 同上）；updateRole 改 role 枚举（MEMBER/ADMIN/OWNER），
 *                          改角色权限判断需同步 enums/TeamMemberRole 与权限校验；updateStatus 置 0 表示退出/被移除，
 *                          与团队解散（TeamMapper.dissolve）联动
 * - 【习惯】用户所在正常团队数     → countTeamsByUserId（XML 同上）；统计 status=1 的成员记录，用于创建团队数上限校验，
 *                          改上限规则需同步 TeamService
 */
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
