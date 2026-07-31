/*
 * 管理后台（M7）类型定义 —— 对应后端 dto/admin/ 下的所有 DTO。
 *
 * 设计思路：
 * 与后端 AdminUserResponse / StatusRequest / QuotaRequest 等 DTO 一一对应，
 * 前端通过 Axios 响应拦截器解包 Result<T> 后直接拿到这些类型。
 */

/**
 * 管理员用户管理响应 —— 对应用户列表/管理员列表返回的数据。
 * 后端返回的是 List<AdminUserResponse>，每个元素包含用户的所有管理可见信息。
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
  role: AdminRole
  quota: number
  totalQuota: number
  adminBonusQuota: number
  rewardQuota: number
  usedSpace: number
  isVip: boolean
  status: AdminUserStatus
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
  status: AdminUserStatus
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
  role: AdminRole
}

/** 修改管理员角色请求体 */
export interface UpdateRoleRequest {
  role: AdminRole
}

/**
 * 用户状态枚举 —— 对应后端 com.cloud.backend.enums.UserStatus。
 * DISABLED=0：手动禁用的用户（不可登录）
 * NORMAL=1：正常状态
 * LOCKED=2：登录失败次数过多自动锁定
 * INACTIVE=3：长期未登录
 */
export enum AdminUserStatus {
  DISABLED = 0,
  NORMAL = 1,
  LOCKED = 2,
  INACTIVE = 3,
}

/** 用户状态的中文映射，用于表格展示 */
export const AdminUserStatusLabel: Record<AdminUserStatus, string> = {
  [AdminUserStatus.DISABLED]: '已禁用',
  [AdminUserStatus.NORMAL]: '正常',
  [AdminUserStatus.LOCKED]: '已锁定',
  [AdminUserStatus.INACTIVE]: '未活跃',
}

/** 用户状态的 Element Plus Tag 类型映射 */
export const AdminUserStatusType: Record<AdminUserStatus, string> = {
  [AdminUserStatus.DISABLED]: 'danger',
  [AdminUserStatus.NORMAL]: 'success',
  [AdminUserStatus.LOCKED]: 'warning',
  [AdminUserStatus.INACTIVE]: 'info',
}

/**
 * 角色枚举 —— 复用 types/user.ts 中的 Role 定义，但用更准确的名字避免混淆。
 * 实际上后端 AdminUserResponse.role 返回的是 Role 枚举，前端直接用相同的数值。
 */
export enum AdminRole {
  USER = 0,
  OPERATOR = 10,
  ADMIN = 20,
  SUPER_ADMIN = 100,
}

export const AdminRoleLabel: Record<AdminRole, string> = {
  [AdminRole.USER]: '用户',
  [AdminRole.OPERATOR]: '运营',
  [AdminRole.ADMIN]: '管理员',
  [AdminRole.SUPER_ADMIN]: '超级管理员',
}
