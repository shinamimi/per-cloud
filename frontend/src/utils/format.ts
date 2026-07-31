/*
 * 容量格式化工具 —— 统一的字节 → 可读单位显示。
 *
 * 设计思路：
 * 管理端多个页面（用户管理、仪表盘）都需展示容量（配额/已用空间），
 * 单位换算逻辑集中在此，页面只声明当前选择的单位并调用 formatSize()。
 *
 * 为什么不做"自动单位"而要求页面显式传单位？
 * 自动单位（KB→MB→GB 自适应）在不同行之间显示单位不一致，难以对比；
 * 由用户/页面显式选择单位，整列单位统一，更适合表格场景。
 */

/** 容量显示单位 */
export type SizeUnit = 'KB' | 'MB' | 'GB'

/** 可选项，供 el-select 渲染 */
export const SIZE_UNITS: SizeUnit[] = ['KB', 'MB', 'GB']

/** 各单位的字节基数（1 KB = 1024 字节，2 进制） */
export const UNIT_BYTES: Record<SizeUnit, number> = {
  KB: 1024,
  MB: 1024 * 1024,
  GB: 1024 * 1024 * 1024,
}

/**
 * 将字节数按指定单位格式化为字符串。
 * - 数值 >= 100 时取整（避免过长的分数）
 * - 否则保留最多 2 位小数，并去掉末尾多余的 0（10.50 → 10.5，10.00 → 10）
 *
 * @param bytes 字节数
 * @param unit  目标单位（默认 GB）
 */
export function formatSize(bytes: number, unit: SizeUnit = 'GB'): string {
  const value = bytes / UNIT_BYTES[unit]
  if (value === 0) return '0'
  if (value >= 100) return String(Math.round(value))
  return value.toFixed(2).replace(/\.?0+$/, '')
}

/**
 * 自动选择单位格式化 —— 用于只读展示场景（表格列、统计卡片）。
 * 按数值大小自动选择 B/KB/MB/GB/TB，带单位后缀（如 "1.5 GB"）。
 */
export function formatBytesAuto(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}
