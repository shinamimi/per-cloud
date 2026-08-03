/*
 * 字典（Meta）类型定义 —— 对应 GET /api/meta/options 返回结构。
 *
 * 设计思路：
 * - 业务枚举（用户状态、角色、分享状态等）统一由后端字典接口下发
 * - 只返回业务语义 { value, label }，颜色/图标/Tag 类型等 UI 样式由前端维护
 * - value 与后端 Java 枚举的 name 一致（如 "NORMAL"、"ADMIN"），JSON 字符串
 */

/** 字典选项 —— 每组都是 { value, label } 数组 */
export interface OptionItem {
  value: string
  label: string
}

/** 字典响应体 —— 按组（group）组织，新增枚举组无需修改接口定义 */
export interface MetaGroups {
  groups: Record<string, OptionItem[]>
}

/** 常用字典组名常量 —— 避免魔法字符串散落在业务代码中 */
export const MetaGroup = {
  USER_STATUS: 'userStatus',
  ROLE: 'role',
  SHARE_STATUS: 'shareStatus',
  OPERATION_TYPE: 'operationType',
} as const
