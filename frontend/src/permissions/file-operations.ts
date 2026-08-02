/*
 * 文件操作规则表 —— 文件模块的 can() 规则。
 *
 * 设计思路（依据 docs/frontend-standard.md 5.x）：
 * - 页面只调用 can()，不关心规则细节
 * - 文件操作的「目标状态」维度由文件类型（FILE / DIRECTORY）充当：
 *   下载/预览仅适用于文件，重命名/移动/复制/删除对两者均适用
 * - 资源级能力（canDelete 等）由后端业务接口返回（FileItem.capabilities），
 *   页面在 can() 推导通过后，再与 capabilities 取交集
 */
import type { OperationRule } from '@/permissions/admin-operations'

/**
 * 文件操作规则表。
 * targetStatuses 在此处表示允许的文件类型（'*' = 全部类型）。
 */
export const FILE_OPERATIONS = {
  rename: { minRole: 'USER', targetStatuses: ['*'], type: 'default' },
  move: { minRole: 'USER', targetStatuses: ['*'], type: 'default' },
  copy: { minRole: 'USER', targetStatuses: ['*'], type: 'default' },
  delete: { minRole: 'USER', targetStatuses: ['*'], type: 'danger' },
  download: { minRole: 'USER', targetStatuses: ['FILE'], type: 'default' },
  preview: { minRole: 'USER', targetStatuses: ['FILE'], type: 'default' },
  share: { minRole: 'USER', targetStatuses: ['*'], type: 'default' },
} as const satisfies Record<string, OperationRule>

/** 文件操作名联合类型 */
export type FileOperation = keyof typeof FILE_OPERATIONS
