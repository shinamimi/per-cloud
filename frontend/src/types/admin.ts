/*
 * 管理后台（M7）类型定义 —— 对应后端 dto/admin/ 下的所有 DTO。
 *
 * 设计思路：
 * - 后端 Spring Boot 默认将 Java 枚举序列化为 JSON 字符串（name），
 *   因此 role / status 在此处声明为字符串字面量联合类型。
 * - 字符串值即字典接口（GET /api/meta/options）中的 value，
 *   label 展示统一从 metaStore 字典组获取（见 docs/frontend-standard.md）。
 */

/** 用户状态字符串值 —— 对应后端 UserStatus 枚举 name */
export type UserStatusKey = 'NORMAL' | 'DISABLED' | 'LOCKED' | 'INACTIVE'

/** 角色字符串值 —— 对应后端 Role 枚举 name */
export type RoleKey = 'USER' | 'OPERATOR' | 'ADMIN' | 'SUPER_ADMIN'

/**
 * 管理员用户管理响应 —— 对应用户列表/管理员列表返回的数据。
 *
 * 字段说明：
 * - totalQuota: base quota + admin bonus quota + reward quota 之和
 * - quota: 用户基础配额
 * - adminBonusQuota: 管理员额外分配的配额（通过此页面设置）
 * - rewardQuota: 奖励配额
 */
export interface AdminUserResponse {
  id: number
  username: string
  email: string
  nickname: string
  avatar: string
  role: RoleKey
  quota: number
  totalQuota: number
  adminBonusQuota: number
  rewardQuota: number
  usedSpace: number
  isVip: boolean
  status: UserStatusKey
  createdAt: string
}

/** 仪表盘统计数据 */
export interface AdminDashboardStats {
  userCount: number
  fileCount: number
  totalSize: number
  totalQuota: number
  usagePercent: number
}

/** 修改用户状态请求体 */
export interface StatusRequest {
  status: UserStatusKey
}

/** 修改配额请求体 —— adminBonusQuota 是管理员额外分配的额度（字节） */
export interface QuotaRequest {
  adminBonusQuota: number
}

/** 重置密码请求体 —— 密码规则：9+ 位，必须包含字母和数字 */
export interface AdminResetPasswordRequest {
  newPassword: string
}

/** 创建管理员请求体 */
export interface CreateAdminRequest {
  username: string
  password: string
  email: string
  nickname?: string
  role: RoleKey
}

/** 修改管理员角色请求体 */
export interface UpdateRoleRequest {
  role: RoleKey
}

/** 候选管理员 —— 穿梭器左侧列表项（对应 GET /api/admin/admins/candidates） */
export interface AdminCandidate {
  id: number
  username: string
  nickname: string | null
}

/** 批量角色变更项 —— 对应 PUT /api/admin/admins/batch 请求体元素 */
export interface AdminRoleChange {
  userId: number
  newRole: RoleKey
}

/**
 * 用户状态 → Tag 类型映射 —— 前端维护的 UI 展示样式。
 * 字典接口只返回 value + label，颜色等样式属于前端职责（见 frontend-standard.md）。
 */
export const USER_STATUS_TAG_TYPE: Record<UserStatusKey, string> = {
  NORMAL: 'success',
  DISABLED: 'danger',
  LOCKED: 'warning',
  INACTIVE: 'info',
}
