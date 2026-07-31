/*
 * 权限推导函数 —— can()。
 *
 * 设计思路（详见 docs/frontend-standard.md）：
 * 页面只调用 can(operation, currentRole, targetStatus, rules)，
 * 不关心规则表内部实现。推导逻辑两步：
 * 1. 角色等级门槛：当前角色等级 >= 规则要求的 minRole 等级
 * 2. 目标状态门槛：规则 targetStatuses 含 '*' 或包含目标行状态
 *
 * 使用示例：
 *   can('disable', userStore.role, row.status, USER_OPERATIONS)  // true/false
 *
 * 与后端能力的边界：
 * 本函数仅推导「角色 + 状态」可推断的基础操作；
 * 资源相关能力（canDelete / canShare）由后端业务接口返回，不走此函数。
 */
import { ROLE_LEVEL, toRoleLevel, type RoleKey } from '@/permissions/role'
import type { OperationRule } from '@/permissions/admin-operations'

/**
 * 判断当前用户能否对指定状态的目标执行某操作。
 *
 * @param operation     操作名（如 'disable'）
 * @param currentRole   当前登录者角色（字符串 "ADMIN" 或数值 20 均可）
 * @param targetStatus  目标行状态（如 'NORMAL'；无状态目标传 undefined）
 * @param rules         操作规则表
 */
export function can(
  operation: string,
  currentRole: number | string | null | undefined,
  targetStatus: string | undefined,
  rules: Record<string, OperationRule>,
): boolean {
  const rule = rules[operation]
  if (!rule) return false

  // 角色等级门槛：当前等级不足直接无权
  if (toRoleLevel(currentRole as RoleKey) < ROLE_LEVEL[rule.minRole]) return false

  // 状态门槛：'*' 不限制状态，否则目标状态必须在允许集合内
  if (rule.targetStatuses.includes('*')) return true
  return !!targetStatus && rule.targetStatuses.includes(targetStatus)
}
