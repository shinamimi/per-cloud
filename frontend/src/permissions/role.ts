/*
 * 角色等级定义 —— 前端权限推导的唯一依据。
 *
 * 设计思路：
 * 不直接比较角色字符串，统一换算为数值等级（ROLE_LEVEL），
 * 规则表（admin-operations.ts）中的 minRole 声明的是等级下限。
 *
 * 两种角色表示法：
 * - 字符串键（ROLE_LEVEL 的 key）：字典/管理接口返回的 "USER" / "OPERATOR" 等
 * - 数值（角色 value）：LoginResponse.role 返回的 0 / 10 / 20 / 100
 * toRoleLevel() 统一转换，can() 内部只比较数值。
 */

/** 角色等级表 —— 数值越大权限越高 */
export const ROLE_LEVEL = {
  USER: 1,
  OPERATOR: 2,
  ADMIN: 3,
  SUPER_ADMIN: 4,
} as const

/** 角色字符串键 */
export type RoleKey = keyof typeof ROLE_LEVEL

/** 角色数值 value → 等级 映射（对应后端 Role.getValue()） */
const ROLE_VALUE_TO_LEVEL: Record<number, number> = {
  0: ROLE_LEVEL.USER, // 对应后端 USER(0)
  10: ROLE_LEVEL.OPERATOR, // 对应后端 OPERATOR(10)
  20: ROLE_LEVEL.ADMIN, // 对应后端 ADMIN(20)
  100: ROLE_LEVEL.SUPER_ADMIN, // 对应后端 SUPER_ADMIN(100)
}

/**
 * 将角色统一转换为数值等级。
 * 兼容字符串（"ADMIN"）和数值（20）两种来源，未知值按最低等级 USER 处理。
 */
export function toRoleLevel(role: RoleKey | number | null | undefined): number {
  if (typeof role === 'number') {
    return ROLE_VALUE_TO_LEVEL[role] ?? ROLE_LEVEL.USER
  }
  if (role && role in ROLE_LEVEL) {
    return ROLE_LEVEL[role as RoleKey]
  }
  return ROLE_LEVEL.USER
}
