<!--
  ShareManageView —— 我的分享管理（/shares）。
  列表展示全部分享（含已取消），支持复制链接、修改有效期、取消分享、跳转访客页。
  对应 docs/share-module.md §五（我的分享管理）。
-->
<template>
  <div class="share-manage">
    <div class="header">
      <h3>我的分享</h3>
      <span class="hint">共 {{ shares.length }} 条（含已取消）</span>
    </div>

    <el-table v-loading="loading" :data="shares" stripe style="width: 100%">
      <el-table-column label="名称" min-width="220">
        <template #default="{ row }">
          <el-icon :size="16" :color="row.isDir ? '#e6a23c' : '#409eff'">
            <Folder v-if="row.isDir" />
            <Document v-else />
          </el-icon>
          <span class="name-text">{{ row.name || '（文件已删除）' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="150">
        <template #default="{ row }">
          {{ row.expireTime ? row.expireTime.slice(0, 16).replace('T', ' ') : '永久' }}
        </template>
      </el-table-column>
      <el-table-column label="提取码" width="90">
        <template #default="{ row }">
          {{ row.requirePassword ? '有' : '无' }}
        </template>
      </el-table-column>
      <el-table-column label="下载" width="120">
        <template #default="{ row }">
          {{ row.downloadCount }}<span v-if="row.maxDownload > 0"> / {{ row.maxDownload }}</span> 次
        </template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleCopy(row)">复制链接</el-button>
          <el-button
            v-if="row.status === 'NORMAL'"
            link
            type="primary"
            size="small"
            @click="handleEditExpire(row)"
          >改期</el-button>
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

    <el-dialog v-model="expireDialogVisible" title="修改有效期" width="420px">
      <el-form label-width="100px">
        <el-form-item label="有效期">
          <el-radio-group v-model="expireForm.validType">
            <el-radio value="PERMANENT">永久有效</el-radio>
            <el-radio value="DAYS">按天</el-radio>
          </el-radio-group>
          <el-input-number
            v-if="expireForm.validType === 'DAYS'"
            v-model="expireForm.validDays"
            :min="1"
            :max="365"
            class="days-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expireDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleExpireSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import { cancelShare, deleteShareRecord, listShares, updateShareExpire } from '@/api/share'
import type { ShareItem, ShareStatusKey, ShareValidType } from '@/types/share'

const router = useRouter()
const loading = ref(false)
const shares = ref<ShareItem[]>([])

const expireDialogVisible = ref(false)
const saving = ref(false)
const expireTarget = ref<ShareItem | null>(null)
const expireForm = reactive({
  validType: 'PERMANENT' as ShareValidType,
  validDays: 7,
})

function statusLabel(status: ShareStatusKey): string {
  return { NORMAL: '生效中', EXPIRED: '已过期', CANCELED: '已取消', EXHAUSTED: '已用尽' }[status] ?? status
}

function statusTagType(status: ShareStatusKey): 'success' | 'info' | 'warning' | 'danger' {
  return status === 'NORMAL' ? 'success' : status === 'EXPIRED' ? 'warning' : 'info'
}

function shareLink(row: ShareItem): string {
  return `${location.origin}${location.pathname}#/s/${row.shareToken}`
}

function handleCopy(row: ShareItem) {
  const link = shareLink(row)
  navigator.clipboard?.writeText(link).catch(() => {})
  ElMessage.success('链接已复制')
}

function handleEditExpire(row: ShareItem) {
  expireTarget.value = row
  expireForm.validType = 'PERMANENT'
  expireForm.validDays = 7
  expireDialogVisible.value = true
}

async function handleExpireSubmit() {
  if (!expireTarget.value) return
  if (expireForm.validType === 'DAYS' && (!expireForm.validDays || expireForm.validDays <= 0)) {
    ElMessage.warning('请输入有效天数')
    return
  }
  saving.value = true
  try {
    await updateShareExpire(expireTarget.value.id, {
      validType: expireForm.validType,
      validDays: expireForm.validType === 'DAYS' ? expireForm.validDays : undefined,
    })
    ElMessage.success('有效期已更新')
    expireDialogVisible.value = false
    await load()
  } catch {
    // 错误已由拦截器提示
  } finally {
    saving.value = false
  }
}

async function handleCancel(row: ShareItem) {
  try {
    await ElMessageBox.confirm(`确定取消分享「${row.name || '该文件'}」吗？访客将无法继续访问。`, '取消分享', {
      confirmButtonText: '确定',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await cancelShare(row.id)
    ElMessage.success('已取消分享')
    await load()
  } catch {
    // 错误已由拦截器提示
  }
}

async function handleDelete(row: ShareItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除分享「${row.name || '该文件'}」的记录吗？删除后分享链接将失效且无法恢复。`,
      '删除分享记录',
      { confirmButtonText: '确定删除', cancelButtonText: '再想想', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteShareRecord(row.id)
    ElMessage.success('分享记录已删除')
    await load()
  } catch {
    // 错误已由拦截器提示
  }
}

async function load() {
  loading.value = true
  try {
    shares.value = await listShares()
  } catch {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.share-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header h3 {
  margin: 0;
}

.hint {
  font-size: 12px;
  color: #909399;
}

.name-text {
  margin-left: 6px;
}

.days-input {
  width: 140px;
  margin-left: 12px;
}
</style>
