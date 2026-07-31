<!--
  TransferQueue —— 传输队列面板（抽屉）。
  展示上传/批量下载任务：状态、进度条、大小；可清理已完成/失败任务。
-->
<template>
  <el-drawer v-model="visible" title="传输队列" size="380px">
    <div v-if="uploadStore.tasks.length === 0" class="queue-empty">
      <el-empty description="暂无传输任务" :image-size="80" />
    </div>

    <div v-for="task in uploadStore.tasks" :key="task.id" class="task-item">
      <div class="task-header">
        <div class="task-name" :title="task.name">
          <el-icon class="task-icon"><component :is="taskIcon(task)" /></el-icon>
          <span>{{ task.name }}</span>
        </div>
        <div class="task-meta">
          <el-tag :type="statusTag(task.status)" size="small">{{ statusLabel(task.status) }}</el-tag>
          <el-button link type="primary" size="small" @click="handleRemove(task.id)">
            移除
          </el-button>
        </div>
      </div>
      <div class="task-body">
        <el-progress
          :percentage="task.progress"
          :status="progressStatus(task.status)"
          :stroke-width="6"
        />
        <div class="task-size">{{ formatBytesAuto(task.size) }}</div>
      </div>
      <div v-if="task.error" class="task-error">{{ task.error }}</div>
    </div>

    <template #footer>
      <el-button size="small" :disabled="uploadStore.tasks.length === 0" @click="uploadStore.clearFinished()">
        清除已完成
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Upload, Download } from '@element-plus/icons-vue'
import { useUploadStore } from '@/stores/upload'
import { formatBytesAuto } from '@/utils/format'
import type { TransferTaskStatus } from '@/types/file'

const visible = ref(false)

const uploadStore = useUploadStore()

/** 打开队列面板（工具栏入口调用） */
function open() {
  visible.value = true
}

defineExpose({ open })

/* ========== UI 映射（前端维护） ========== */

const STATUS_LABEL: Record<TransferTaskStatus, string> = {
  pending: '等待中',
  hashing: '校验中',
  uploading: '上传中',
  merging: '合并中',
  packing: '打包中',
  completed: '已完成',
  failed: '失败',
}

const STATUS_TAG: Record<TransferTaskStatus, 'primary' | 'success' | 'danger' | 'info'> = {
  pending: 'info',
  hashing: 'primary',
  uploading: 'primary',
  merging: 'primary',
  packing: 'primary',
  completed: 'success',
  failed: 'danger',
}

function statusLabel(status: TransferTaskStatus): string {
  return STATUS_LABEL[status]
}

function handleRemove(id: string) {
  uploadStore.removeTask(id)
}

function statusTag(status: TransferTaskStatus) {
  return STATUS_TAG[status]
}

function progressStatus(status: TransferTaskStatus): 'success' | 'exception' | undefined {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return undefined
}

function taskIcon(task: { kind: 'upload' | 'download' }) {
  return task.kind === 'upload' ? Upload : Download
}
</script>

<style scoped>
.queue-empty {
  padding-top: 40px;
}

.task-item {
  border-bottom: 1px solid #f0f2f5;
  padding: 12px 0;
}

.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.task-name {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  color: #303133;
}

.task-name span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-icon {
  color: #909399;
  flex-shrink: 0;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.task-body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-body .el-progress {
  flex: 1;
}

.task-size {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.task-error {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}

.task-meta :deep(.el-button) {
  margin-left: 4px;
}
</style>
