/*
 * 用户管理模块（M2）类型定义 —— 对应用户资料、配额等 DTO。
 *
 * 对应后端 DDD 文档中的 UserProfileResponse / QuotaResponse / UserUpdateRequest / PasswordUpdateRequest。
 */

/** 用户资料响应体 */
export interface UserProfile {
  id: number
  username: string
  email: string
  nickname: string
  avatar: string
  role: number
  quota: number
  usedSpace: number
  status: number
  createdAt: string
}

/** 更新个人资料请求 */
export interface UserUpdateRequest {
  nickname?: string
  avatar?: string
}

/** 修改密码请求 */
export interface PasswordUpdateRequest {
  oldPassword: string
  newPassword: string
}

/** 空间配额响应 */
export interface QuotaResponse {
  quota: number
  usedSpace: number
  usagePercent: number
}

/**
 * 用户角色枚举 —— 对应后端 com.cloud.backend.enums.Role。
 * value 越大权限越高，用于前端路由鉴权（如 admin 页面需要 role >= ADMIN）。
 */
export enum Role {
  USER = 0,
  OPERATOR = 10,
  ADMIN = 20,
  SUPER_ADMIN = 100,
}
