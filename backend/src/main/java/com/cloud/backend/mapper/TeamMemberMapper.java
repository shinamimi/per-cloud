package com.cloud.backend.mapper;

import com.cloud.backend.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TeamMemberMapper {

    int insert(TeamMember teamMember);

    List<TeamMember> findByTeamId(Long teamId);

    long countByTeamId(Long teamId);
}
