<!--
  UserPreviewTable —— 只读用户预览表格（轻量组件，不含任何操作列）。
  用于系统配置中心"老用户配额调整"的明细弹窗。
  设计思路：不直接复用 AdminUserView 的完整表格，避免耦合操作逻辑。
-->
<template>
  <el-table :data="users" v-loading="loading" stripe style="width: 100%" size="default">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="username" label="用户名" min-width="110" />
    <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
    <el-table-column prop="role" label="角色" width="110">
      <template #default="{ row }">
        <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="90">
      <template #default="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="当前配额" width="110">
      <template #default="{ row }">{{ formatBytesAuto(row.quota) }}</template>
    </el-table-column>
    <el-table-column label="新配额" width="110">
      <template #default="{ row }">
        <el-tag size="small" type="info">{{ formatBytesAuto(targetQuotaOf(row)) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="注册时间" width="160">
      <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import type { AdminUserResponse, RoleKey, UserStatusKey } from '@/types/admin'
import { USER_STATUS_TAG_TYPE } from '@/types/admin'
import { MetaGroup } from '@/types/meta'
import { useMetaStore } from '@/stores/meta'
import { formatBytesAuto } from '@/utils/format'

/**
 * 预览表格 props：
 * - users：受影响用户列表
 * - targetQuotaUser / targetQuotaVip：本次调整的目标配额（按用户 VIP 区分展示）
 * - loading：加载中状态
 */
const props = defineProps<{
  users: AdminUserResponse[]
  targetQuotaUser: number
  targetQuotaVip: number
  loading?: boolean
}>()

const metaStore = useMetaStore()

/** 按 VIP 区分展示目标配额 */
function targetQuotaOf(row: AdminUserResponse): number {
  return row.isVip ? props.targetQuotaVip : props.targetQuotaUser
}

function roleLabel(role: RoleKey): string {
  return metaStore.getGroup(MetaGroup.ROLE).find((opt) => opt.value === role)?.label ?? role
}

function roleTagType(role: RoleKey): string {
  const map: Record<RoleKey, string> = {
    USER: 'info',
    OPERATOR: 'warning',
    ADMIN: 'danger',
    SUPER_ADMIN: 'danger',
  }
  return map[role] ?? 'info'
}

function statusLabel(status: UserStatusKey): string {
  return metaStore.getGroup(MetaGroup.USER_STATUS).find((opt) => opt.value === status)?.label ?? status
}

function statusTagType(status: UserStatusKey): string {
  return USER_STATUS_TAG_TYPE[status] ?? 'info'
}

onMounted(() => {
  metaStore.loadIfNeeded()
})
</script>
