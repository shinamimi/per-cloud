<!--
  RecycleBinDialog —— 回收站对话框。
  展示已删除文件（GET /api/files/recycle-bin，List 无分页），
  支持恢复（原名还原，若冲突自动加后缀）与彻底删除（释放 MinIO 对象与配额）。
-->
<template>
  <el-dialog
    v-model="visible"
    title="回收站"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
    @open="handleOpen"
  >
    <el-table :data="items" v-loading="loading" empty-text="回收站是空的" row-key="id" stripe>
      <el-table-column label="名称" min-width="220">
        <template #default="{ row }">
          <div class="recycle-name">
            <el-icon :size="16" :class="row.type === 1 ? 'icon-dir' : 'icon-file'">
              <component :is="row.type === 1 ? Folder : Document" />
            </el-icon>
            <span class="recycle-name-text" :title="row.originalName">{{ row.originalName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="110">
        <template #default="{ row }">
          {{ row.type === 1 ? '—' : formatBytesAuto(row.size) }}
        </template>
      </el-table-column>
      <el-table-column label="删除时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.deletedTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleRestore(row)">恢复</el-button>
          <el-button link type="danger" size="small" @click="handlePurge(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import { getRecycleBinList, restoreRecycle, purgeRecycle } from '@/api/file'
import { formatBytesAuto } from '@/utils/format'
import type { RecycleBinItem } from '@/types/file'

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const visible = ref(false)

const fileStore = useFileStore()
const items = ref<RecycleBinItem[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await getRecycleBinList()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}

function handleOpen() {
  load()
}

async function handleRestore(row: RecycleBinItem) {
  try {
    await restoreRecycle(row.id)
    ElMessage.success(`已恢复「${row.originalName}」`)
    await load()
    await fileStore.refresh()
    await fileStore.loadTree(true)
  } catch {
    // 错误已在拦截器中提示
  }
}

async function handlePurge(row: RecycleBinItem) {
  try {
    await ElMessageBox.confirm(
      `确定彻底删除「${row.originalName}」吗？该操作不可恢复。`,
      '彻底删除',
      { confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await purgeRecycle(row.id)
    ElMessage.success('已彻底删除')
    await load()
  } catch {
    // 错误已在拦截器中提示
  }
}

/** 时间展示：yyyy-MM-dd HH:mm */
function formatTime(value: string): string {
  return value?.slice(0, 16).replace('T', ' ') ?? ''
}

defineExpose({ open: () => (visible.value = true) })
</script>

<style scoped>
.recycle-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recycle-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-dir {
  color: #e6a23c;
}

.icon-file {
  color: #909399;
}
</style>
