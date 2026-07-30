package com.cloud.backend.mapper;

import com.cloud.backend.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TeamMapper {

    int insert(Team team);

    Team findById(Long id);

    List<Team> findAll();

    int update(Team team);

    int dissolve(Long id);
}
