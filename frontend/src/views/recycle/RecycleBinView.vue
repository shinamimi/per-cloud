<!--
  RecycleBinView —— 回收站页面（/recycle-bin）。
  与一般文件列表同等的查看方式（全宽表格 + 分页），支持恢复与彻底删除。
  数据来自 GET /api/files/recycle-bin（后端全量返回，前端分页展示）。
-->
<template>
  <div class="recycle-view">
    <div class="recycle-header">
      <el-button @click="router.push('/files')">
        <el-icon><Back /></el-icon>
        <span>返回文件</span>
      </el-button>
      <h2 class="page-title">回收站</h2>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="pagedItems" v-loading="loading" empty-text="回收站是空的" row-key="id" stripe>
        <el-table-column label="名称" min-width="260">
          <template #default="{ row }">
            <div class="recycle-name">
              <el-icon :size="18" :class="row.type === 1 ? 'icon-dir' : 'icon-file'">
                <component :is="row.type === 1 ? Folder : Document" />
              </el-icon>
              <span class="recycle-name-text" :title="row.originalName">{{ row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ row.type === 1 ? '—' : formatBytesAuto(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="删除时间" width="170">
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

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="items.length"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Folder, Document } from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import { getRecycleBinList, restoreRecycle, purgeRecycle } from '@/api/file'
import { formatBytesAuto } from '@/utils/format'
import type { RecycleBinItem } from '@/types/file'

const router = useRouter()
const fileStore = useFileStore()

const items = ref<RecycleBinItem[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)

/** 前端分页切片（后端接口为全量列表） */
const pagedItems = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return items.value.slice(start, start + pageSize.value)
})

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

onMounted(load)
</script>

<style scoped>
.recycle-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recycle-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
