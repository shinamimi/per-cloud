<!--
  TeamRecycleView —— 团队回收站页面（/teams/:id/recycle）。
  团队删除的文件/目录集中在此（保留天数取团队配置，默认 30 天）；
  恢复占用团队配额，彻底删除释放对象（引用归零时）与配额。
-->
<template>
  <div class="team-recycle-view">
    <el-page-header @back="router.push(`/teams/${teamId}/files`)">
      <template #title>
        <span class="tr-title">团队回收站</span>
      </template>
    </el-page-header>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="items">
        <el-table-column label="名称" min-width="260">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon v-if="row.type === 1"><Folder /></el-icon>
              <el-icon v-else class="file-icon"><Document /></el-icon>
              <span>{{ row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ row.type === 1 ? '-' : formatBytesAuto(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="deletedTime" label="删除时间" width="170" />
        <el-table-column label="保留至" width="170">
          <template #default="{ row }">{{ row.expireTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRestore(row)">恢复</el-button>
            <el-button link type="danger" @click="handlePurge(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && items.length === 0" description="回收站是空的" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Folder } from '@element-plus/icons-vue'
import {
  getTeamRecycleBin,
  purgeTeamRecycle,
  restoreTeamRecycle,
} from '@/api/team'
import { formatBytesAuto } from '@/utils/format'
import type { RecycleBinItem } from '@/types/file'

const route = useRoute()
const router = useRouter()
const teamId = computed(() => Number(route.params.id))
const items = ref<RecycleBinItem[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await getTeamRecycleBin(teamId.value)
  } finally {
    loading.value = false
  }
}

async function handleRestore(row: RecycleBinItem) {
  await restoreTeamRecycle(teamId.value, row.id)
  ElMessage.success('已恢复')
  await load()
}

async function handlePurge(row: RecycleBinItem) {
  try {
    await ElMessageBox.confirm(
      `确定彻底删除「${row.originalName}」吗？文件数据将不可恢复。`,
      '彻底删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await purgeTeamRecycle(teamId.value, row.id)
  ElMessage.success('已彻底删除')
  await load()
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.team-recycle-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tr-title {
  font-size: 15px;
  font-weight: 600;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.file-icon {
  color: #409eff;
}
</style>
