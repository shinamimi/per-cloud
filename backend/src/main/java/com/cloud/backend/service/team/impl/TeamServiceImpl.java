package com.cloud.backend.service.team.impl;

import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.TeamMapper;
import com.cloud.backend.mapper.TeamMemberMapper;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.team.TeamService;
import com.cloud.backend.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserService userService;
    private final OperationLogService operationLogService;

    public TeamServiceImpl(TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                           UserService userService, OperationLogService operationLogService) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Team findById(Long id) {
        Team team = teamMapper.findById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        return team;
    }

    @Override
    public List<Team> findAll() {
        return teamMapper.findAll();
    }

    @Override
    public void dissolve(Long id, Long operatorId) {
        Team team = findById(id);
        teamMapper.dissolve(id);

        User operator = userService.findById(operatorId);
        OperationLog log = new OperationLog();
        log.setUserId(operatorId);
        log.setOperation(OperationType.TEAM_DISSOLVE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(id);
        log.setDetail("强制解散团队: " + team.getName()
                + " (操作人: " + (operator != null ? operator.getUsername() : operatorId) + ")");
        operationLogService.log(log);
    }
}
