package com.cloud.backend.service.team.impl;

import com.cloud.backend.dto.team.TeamCreateRequest;
import com.cloud.backend.dto.team.TeamMemberResponse;
import com.cloud.backend.dto.team.TeamResponse;
import com.cloud.backend.dto.team.TeamUpdateRequest;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.enums.TeamMemberRole;
import com.cloud.backend.enums.TeamStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.TeamMapper;
import com.cloud.backend.mapper.TeamMemberMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.team.TeamService;
import com.cloud.backend.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 团队服务实现。
 * - 创建团队：OWNER 自动成为成员；配额取配置中心默认值；团队数上限校验
 * - 成员管理：邀请（ADMIN+）/移除（ADMIN+，不能移除 OWNER）/退出（OWNER 不可退出）
 * - 解散：仅 OWNER（用户端），管理端强制解散由 AdminTeamController 直接调用
 */
@Service
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserService userService;
    private final OperationLogService operationLogService;
    private final AdminSettingsService adminSettingsService;

    public TeamServiceImpl(TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                           UserService userService, OperationLogService operationLogService,
                           AdminSettingsService adminSettingsService) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.userService = userService;
        this.operationLogService = operationLogService;
        this.adminSettingsService = adminSettingsService;
    }

    /* ==================== 团队 CRUD ==================== */

    @Override
    @Transactional
    public Team create(Long userId, TeamCreateRequest request) {
        String name = request.getName().trim();
        if (name.isEmpty() || name.length() > 64) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队名称长度需在 1-64 之间");
        }
        if (teamMapper.findByName(name) != null) {
            throw new BusinessException(ErrorCode.TEAM_NAME_DUPLICATE);
        }
        // 每人团队数上限（配置中心 team.max-per-user）
        long myTeams = teamMemberMapper.countTeamsByUserId(userId);
        if (myTeams >= adminSettingsService.getTeamMaxPerUser()) {
            throw new BusinessException(ErrorCode.TEAM_LIMIT_EXCEEDED);
        }

        Team team = new Team();
        team.setName(name);
        team.setOwnerId(userId);
        team.setAvatar(request.getAvatar());
        team.setDescription(request.getDescription());
        team.setStatus(TeamStatus.NORMAL);
        team.setQuota(adminSettingsService.getTeamDefaultQuota());
        team.setUsedSpace(0L);
        teamMapper.insert(team);

        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(userId);
        owner.setRole(TeamMemberRole.OWNER);
        owner.setStatus(1);
        teamMemberMapper.insert(owner);

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.TEAM_CREATE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(team.getId());
        log.setDetail("创建团队: " + team.getName());
        operationLogService.log(log);
        return team;
    }

    @Override
    public TeamResponse update(Long teamId, Long userId, TeamUpdateRequest request) {
        TeamMember actor = requireAdmin(teamId, userId);
        Team team = findById(teamId);
        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            if (name.length() > 64) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "团队名称最长 64 字符");
            }
            Team exist = teamMapper.findByName(name);
            if (exist != null && !exist.getId().equals(teamId)) {
                throw new BusinessException(ErrorCode.TEAM_NAME_DUPLICATE);
            }
            team.setName(name);
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            team.setAvatar(request.getAvatar());
        }
        teamMapper.update(team);
        return findResponse(teamId, userId);
    }

    @Override
    public TeamResponse findResponse(Long teamId, Long userId) {
        requireMember(teamId, userId);
        Team team = findById(teamId);
        User owner = userService.findById(team.getOwnerId());
        return TeamResponse.from(team,
                owner == null ? null : owner.getUsername(),
                teamMemberMapper.countByTeamId(teamId),
                getMyRole(teamId, userId));
    }

    @Override
    public Team findById(Long id) {
        Team team = teamMapper.findById(id);
        if (team == null || team.getStatus() != TeamStatus.NORMAL) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        return team;
    }

    @Override
    public List<Team> findAll() {
        return teamMapper.findAll();
    }

    @Override
    public List<TeamResponse> listMyTeams(Long userId) {
        List<Team> teams = teamMapper.findByUserId(userId);
        List<TeamResponse> responses = new ArrayList<>(teams.size());
        for (Team team : teams) {
            User owner = userService.findById(team.getOwnerId());
            responses.add(TeamResponse.from(team,
                    owner == null ? null : owner.getUsername(),
                    teamMemberMapper.countByTeamId(team.getId()),
                    getMyRole(team.getId(), userId)));
        }
        return responses;
    }

    @Override
    @Transactional
    public void dissolve(Long teamId, Long operatorId) {
        requireOwner(teamId, operatorId);
        Team team = findById(teamId);
        teamMapper.dissolve(teamId);
        // 成员记录一并退出（status 置 0），防止解散后残留"正常成员"
        for (TeamMember member : teamMemberMapper.findByTeamId(teamId)) {
            teamMemberMapper.updateStatus(member.getId(), 0);
        }

        User operator = userService.findById(operatorId);
        OperationLog log = new OperationLog();
        log.setUserId(operatorId);
        log.setOperation(OperationType.TEAM_DISSOLVE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(teamId);
        log.setDetail("解散团队: " + team.getName()
                + " (操作人: " + (operator != null ? operator.getUsername() : operatorId) + ")");
        operationLogService.log(log);
    }

    /* ==================== 成员管理 ==================== */

    @Override
    public List<TeamMemberResponse> listMembers(Long teamId, Long userId) {
        requireMember(teamId, userId);
        List<TeamMember> members = teamMemberMapper.findByTeamId(teamId);
        List<TeamMemberResponse> responses = new ArrayList<>(members.size());
        for (TeamMember member : members) {
            responses.add(TeamMemberResponse.from(member, userService.findById(member.getUserId())));
        }
        return responses;
    }

    @Override
    @Transactional
    public void invite(Long teamId, Long operatorId, List<Long> userIds) {
        requireAdmin(teamId, operatorId);
        Team team = findById(teamId);
        long current = teamMemberMapper.countByTeamId(teamId);
        int maxMembers = adminSettingsService.getTeamMaxMembers();
        int added = 0;
        for (Long targetId : userIds) {
            if (targetId == null || targetId.equals(operatorId)) {
                continue;
            }
            User target = userService.findById(targetId);
            if (target == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (teamMemberMapper.findByTeamIdAndUserId(teamId, targetId) != null) {
                continue; // 已在团队中，幂等跳过
            }
            if (current + added >= maxMembers) {
                throw new BusinessException(ErrorCode.TEAM_MEMBER_LIMIT_EXCEEDED);
            }
            TeamMember member = new TeamMember();
            member.setTeamId(teamId);
            member.setUserId(targetId);
            member.setRole(TeamMemberRole.MEMBER);
            member.setStatus(1);
            teamMemberMapper.insert(member);
            added++;

            OperationLog log = new OperationLog();
            log.setUserId(operatorId);
            log.setOperation(OperationType.TEAM_INVITE);
            log.setTargetType(TargetType.TEAM);
            log.setTargetId(teamId);
            log.setDetail("邀请成员: " + target.getUsername() + " 加入团队: " + team.getName());
            operationLogService.log(log);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, Long operatorId, Long targetUserId) {
        TeamMember actor = requireAdmin(teamId, operatorId);
        if (targetUserId == null || targetUserId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED, "不能移除自己，请使用退出团队");
        }
        TeamMember target = teamMemberMapper.findByTeamIdAndUserId(teamId, targetUserId);
        if (target == null || target.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
        if (target.getRole() == TeamMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED, "不能移除团队所有者");
        }
        // ADMIN 不能移除其他 ADMIN（仅 OWNER 可）
        if (actor.getRole() != TeamMemberRole.OWNER && target.getRole() == TeamMemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED, "仅所有者可移除管理员");
        }
        teamMemberMapper.updateStatus(target.getId(), 0);

        Team team = findById(teamId);
        User targetUser = userService.findById(targetUserId);
        OperationLog log = new OperationLog();
        log.setUserId(operatorId);
        log.setOperation(OperationType.TEAM_REMOVE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(teamId);
        log.setDetail("移除成员: " + (targetUser == null ? targetUserId : targetUser.getUsername())
                + " 从团队: " + team.getName());
        operationLogService.log(log);
    }

    @Override
    @Transactional
    public void leave(Long teamId, Long userId) {
        TeamMember member = teamMemberMapper.findByTeamIdAndUserId(teamId, userId);
        if (member == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }
        if (member.getRole() == TeamMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_OWNER_CANNOT_LEAVE);
        }
        teamMemberMapper.updateStatus(member.getId(), 0);

        Team team = findById(teamId);
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.TEAM_LEAVE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(teamId);
        log.setDetail("退出团队: " + team.getName());
        operationLogService.log(log);
    }

    /* ==================== 配额 ==================== */

    @Override
    public long getRemainingQuota(Long teamId) {
        Team team = findById(teamId);
        long used = team.getUsedSpace() == null ? 0 : team.getUsedSpace();
        return Math.max(0, team.getQuota() - used);
    }

    @Override
    public void changeUsedSpace(Long teamId, long delta) {
        teamMapper.updateUsedSpace(teamId, delta);
    }

    @Override
    public void checkQuota(Long teamId, long size) {
        if (size <= 0) {
            return;
        }
        if (size > getRemainingQuota(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_QUOTA_EXCEEDED);
        }
    }

    /* ==================== 权限辅助 ==================== */

    @Override
    public TeamMember getMember(Long teamId, Long userId) {
        TeamMember member = teamMemberMapper.findByTeamIdAndUserId(teamId, userId);
        if (member == null || member.getStatus() != 1) {
            return null;
        }
        return member;
    }

    @Override
    public TeamMember requireMember(Long teamId, Long userId) {
        TeamMember member = getMember(teamId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }
        return member;
    }

    @Override
    public TeamMember requireAdmin(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);
        if (member.getRole().getValue() < TeamMemberRole.ADMIN.getValue()) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED);
        }
        return member;
    }

    @Override
    public TeamMember requireOwner(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);
        if (member.getRole() != TeamMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED);
        }
        return member;
    }

    @Override
    public TeamMemberRole getMyRole(Long teamId, Long userId) {
        TeamMember member = getMember(teamId, userId);
        return member == null ? null : member.getRole();
    }

    /* ==================== 管理端 ==================== */

    @Override
    public long countMembers(Long teamId) {
        return teamMemberMapper.countByTeamId(teamId);
    }

    @Override
    public List<TeamMemberResponse> adminListMembers(Long teamId) {
        List<TeamMember> members = teamMemberMapper.findByTeamId(teamId);
        List<TeamMemberResponse> responses = new ArrayList<>(members.size());
        for (TeamMember member : members) {
            responses.add(TeamMemberResponse.from(member, userService.findById(member.getUserId())));
        }
        return responses;
    }

    @Override
    public void adminUpdateQuota(Long teamId, Long quota) {
        Team team = findById(teamId);
        if (quota == null || quota <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配额必须大于 0");
        }
        long used = team.getUsedSpace() == null ? 0 : team.getUsedSpace();
        if (quota < used) {
            throw new BusinessException(ErrorCode.TEAM_QUOTA_EXCEEDED, "配额不能小于团队已用空间");
        }
        teamMapper.updateQuota(teamId, quota);
    }

    @Override
    @Transactional
    public void adminDissolve(Long teamId, Long operatorId) {
        Team team = findById(teamId);
        teamMapper.dissolve(teamId);
        for (TeamMember member : teamMemberMapper.findByTeamId(teamId)) {
            teamMemberMapper.updateStatus(member.getId(), 0);
        }
        OperationLog log = new OperationLog();
        log.setUserId(operatorId);
        log.setOperation(OperationType.TEAM_DISSOLVE);
        log.setTargetType(TargetType.TEAM);
        log.setTargetId(teamId);
        log.setDetail("管理端强制解散团队: " + team.getName());
        operationLogService.log(log);
    }
}
