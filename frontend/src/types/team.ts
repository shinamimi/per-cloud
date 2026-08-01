/*
 * 团队模块类型定义 —— 对应后端 TeamController（/api/teams）、TeamFileController（/api/teams/{id}/files）
 * 与 dto/team/*。
 *
 * 设计依据（docs/team-module.md + docs/ADR-011）：
 * - 同表 + teamId：团队文件与个人文件共用 t_file，team_id=0 为个人空间
 * - 独立配额（t_team.quota / used_space），成员上传占团队配额
 * - 权限矩阵：OWNER > ADMIN > MEMBER；MEMBER 只能操作自己上传的文件
 * - 团队文件删除进团队回收站（30 天配置，管理端可提前清空）
 */

/** 团队成员角色 —— 对应后端 TeamMemberRole 的 value 语义（OWNER=20 ADMIN=10 MEMBER=0） */
export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'

/** 团队角色可写权限等级（后端 TeamMemberRole.value） */
export const TEAM_ROLE_LEVEL: Record<TeamRole, number> = {
  MEMBER: 0,
  ADMIN: 10,
  OWNER: 20,
}

/** 团队 —— 对应 TeamResponse（GET /api/teams 列表 / 详情） */
export interface Team {
  id: number
  name: string
  avatar: string | null
  description: string | null
  ownerId: number
  ownerName: string | null
  quota: number
  usedSpace: number
  memberCount: number
  myRole: TeamRole | null
  createdAt: string
}

/** 团队成员 —— 对应 TeamMemberResponse */
export interface TeamMember {
  userId: number
  username: string
  nickname: string
  avatar: string
  role: TeamRole
  joinedAt: string
}

/** 创建团队请求体 —— POST /api/teams */
export interface TeamCreateRequest {
  name: string
  description?: string
  avatar?: string
}

/** 更新团队请求体 —— PUT /api/teams/{id} */
export interface TeamUpdateRequest {
  name?: string
  description?: string
  avatar?: string
}

/** 邀请成员请求体 —— POST /api/teams/{id}/members */
export interface TeamInviteRequest {
  userIds: number[]
}
