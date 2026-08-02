<!--
  AdminShareView —— 管理后台分享管理页面（/admin/shares，仅 ADMIN+）。
  查看所有分享（含所有者/文件名/下载次数），控制下载开关、取消、删除记录。
  权限：后端 /api/admin/** 已由 SecurityConfig 拦截（ADMIN 及以上）。
-->
<template>
  <div class="admin-share-view">
    <el-card shadow="never">
      <div class="card-header">
        <span class="card-title">分享管理（{{ shares.length }}）</span>
        <span class="card-hint">下载开关即时生效；取消仅置为已取消，删除记录为物理删除</span>
      </div>

      <el-table v-loading="loading" :data="shares">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="文件" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-cell">
              <el-icon :size="15" :color="row.isDir ? '#e6a23c' : '#409eff'">
                <Folder v-if="row.isDir" />
                <Document v-else />
              </el-icon>
              <span>{{ row.fileName || '（文件已删除）' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所有者" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.ownerName || `用户#${row.userId}` }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下载" width="130">
          <template #default="{ row }">
            {{ row.downloadCount }}<template v-if="row.maxDownload > 0"> / {{ row.maxDownload }}</template> 次
          </template>
        </el-table-column>
        <el-table-column label="允许下载" width="90" fixed="right">
          <template #default="{ row }">
            <el-switch
              :model-value="row.allowDownload"
              :disabled="row.status !== 'NORMAL' || togglingId === row.id"
              @change="(v: string | number | boolean) => handleToggleDownload(row, !!v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'NORMAL'"
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >取消</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除记录</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无分享" :image-size="80" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import { adminCancelShare, adminDeleteShare, adminSetShareDownload, getAdminShares } from '@/api/admin/share'
import type { AdminShareItem } from '@/types/admin'

const loading = ref(false)
const shares = ref<AdminShareItem[]>([])
const togglingId = ref<number | null>(null)

function statusLabel(status: string): string {
  return { NORMAL: '生效中', EXPIRED: '已过期', CANCELED: '已取消', EXHAUSTED: '已用尽' }[status] ?? status
}

function statusTagType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  return status === 'NORMAL' ? 'success' : status === 'EXPIRED' ? 'warning' : 'info'
}

async function handleToggleDownload(row: AdminShareItem, allow: boolean) {
  togglingId.value = row.id
  try {
    await adminSetShareDownload(row.id, allow)
    row.allowDownload = allow
    ElMessage.success(allow ? '已允许下载' : '已禁止下载')
  } catch {
    // 错误已由拦截器提示
  } finally {
    togglingId.value = null
  }
}

async function handleCancel(row: AdminShareItem) {
  try {
    await ElMessageBox.confirm(`确定取消分享「${row.fileName || '该文件'}」吗？访客将无法继续访问。`, '取消分享', {
      confirmButtonText: '确定',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await adminCancelShare(row.id)
    ElMessage.success('已取消分享')
    await load()
  } catch {
    // 错误已由拦截器提示
  }
}

async function handleDelete(row: AdminShareItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除分享「${row.fileName || '该文件'}」的记录吗？删除后分享链接将失效且无法恢复。`,
      '删除分享记录',
      { confirmButtonText: '确定删除', cancelButtonText: '再想想', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await adminDeleteShare(row.id)
    ElMessage.success('分享记录已删除')
    await load()
  } catch {
    // 错误已由拦截器提示
  }
}

async function load() {
  loading.value = true
  try {
    shares.value = await getAdminShares()
  } catch {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-share-view {
  max-width: 1100px;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.card-hint {
  font-size: 12px;
  color: #909399;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
