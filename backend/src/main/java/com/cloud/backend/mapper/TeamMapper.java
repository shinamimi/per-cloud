package com.cloud.backend.mapper;

import com.cloud.backend.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
