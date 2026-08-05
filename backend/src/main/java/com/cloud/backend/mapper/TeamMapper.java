package com.cloud.backend.mapper;

import com.cloud.backend.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 团队 Mapper —— t_team 表。
 * 团队空间以 owner_id 为队长，status 区分正常/解散，quota 独立配额。
 *
 * 设计思路：
 * 1. 用户所在团队经 t_team_member 关联查询（findByUserId 为 INNER JOIN），团队成员关系独立成表
 * 2. 解散为逻辑操作（status=0），成员关系由 TeamMemberMapper.updateStatus 一并置 0
 * 3. 已用空间用原子 SQL 更新（updateUsedSpace），防并发覆盖
 *
 * 修改指引：
 * - 【习惯】创建团队             → insert（XML：src/main/resources/mapper/TeamMapper.xml）；插入后由 Service 同步写 owner
 *                          的成员记录（t_team_member role=OWNER），改字段需同步 XML 与实体
 * - 【习惯】查询团队             → findById / findAll / findByUserId / findByName（XML 同上）；findByUserId 为 t_team 与
 *                          t_team_member 的 INNER JOIN 多表查询（m.status=1 且 t.status=1），改 join 或状态条件需同步 XML；
 *                          findByName 为同名检查（name 无唯一约束，仅业务防重）
 * - 【习惯】更新团队             → update / updateQuota（XML 同上）；update 只改 name/avatar/description，
 *                          updateQuota 用于管理端配额分配，改配额逻辑需同步 AdminTeamService 与 t_team.quota
 * - 【习惯】解散团队             → dissolve（XML 同上）；status 置 0（逻辑解散），需同步 TeamMemberMapper.updateStatus
 *                          将成员一并置 0，与 TeamService 解散流程联动
 * - 【习惯】调整已用空间         → updateUsedSpace（XML 同上）；SQL 为 used_space = GREATEST(used_space + delta, 0) 原子更新、
 *                          负数钳制为 0，上传/删除时由 Service 调用，改语义需同步 XML 与配额校验逻辑
 */
@Mapper
public interface TeamMapper {

    int insert(Team team);

    Team findById(Long id);

    /** 全部团队（管理后台，倒序） */
    List<Team> findAll();

    /** 用户所在正常团队（通过成员表） */
    List<Team> findByUserId(@Param("userId") Long userId);

    /** 更新团队基本信息 */
    int update(Team team);

    /** 解散：status 置 0 */
    int dissolve(Long id);

    /** 团队配额（用于合并入 findById 的展示字段） */
    int updateQuota(@Param("id") Long id, @Param("quota") Long quota);

    /** 团队已用空间 */
    int updateUsedSpace(@Param("id") Long id, @Param("delta") long delta);

    /** 同名检查（创建/改名） */
    Team findByName(@Param("name") String name);
}
