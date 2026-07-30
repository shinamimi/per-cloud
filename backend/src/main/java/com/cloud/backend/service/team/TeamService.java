package com.cloud.backend.service.team;

import com.cloud.backend.entity.Team;
import java.util.List;

public interface TeamService {

    Team findById(Long id);

    List<Team> findAll();

    void dissolve(Long id, Long operatorId);
}
