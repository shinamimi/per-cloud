/*
 * 管理员操作规则表 —— 声明每个操作的最低角色与目标状态限制。
 *
 * 设计思路：
 * - 基础操作（状态/配额/密码重置）可由「当前角色 + 目标行状态」推导显隐
 * - minRole：当前登录者所需的最低角色
 * - targetStatuses：目标行必须满足的状态（'*' 表示不限制）
 * - type：按钮风格（属于前端 UI 展示逻辑，与后端无关）
 *
 * 页面只调用 can()，不直接读取本表：
 *   can('disable', currentUser.role, row.status, USER_OPERATIONS)
 */
import type { RoleKey } from './role'

/** 单条操作规则 */
export interface OperationRule {
  /** 当前登录者最低角色 */
  minRole: RoleKey
  /** 目标行允许的状态集合；'*' 表示任意状态 */
  targetStatuses: string[]
  /** 按钮类型（Element Plus button type） */
  type: 'default' | 'primary' | 'danger' | 'warning' | 'success' | 'info'
}

/** 用户管理操作规则表 */
export const USER_OPERATIONS = {
  /** 禁用用户：仅对正常状态用户可操作 */
  disable: { minRole: 'OPERATOR', targetStatuses: ['NORMAL'], type: 'danger' },
  /** 启用用户：仅对已禁用用户可操作 */
  enable: { minRole: 'OPERATOR', targetStatuses: ['DISABLED'], type: 'primary' },
  /** 解锁：仅对已锁定用户可操作 */
  unlock: { minRole: 'OPERATOR', targetStatuses: ['LOCKED'], type: 'primary' },
  /** 调整配额：任意状态均可 */
  quota: { minRole: 'OPERATOR', targetStatuses: ['*'], type: 'default' },
  /** 重置密码：任意状态均可 */
  resetPwd: { minRole: 'OPERATOR', targetStatuses: ['*'], type: 'default' },
} as const satisfies Record<string, OperationRule>

/** 用户管理操作名联合类型 */
export type UserOperation = keyof typeof USER_OPERATIONS
