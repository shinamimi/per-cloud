<!--
  AdminFileView —— 管理后台全局文件管理页面（/admin/files，仅 ADMIN+）。
  覆盖个人文件 + 团队文件：筛选/多选批量操作/禁用启用/详情侧栏/预览下载/删除进全局回收站。
  回收站 Tab：全局回收站（管理员删除的记录），支持恢复与彻底删除。
  权限：后端 /api/admin/files/** 已由 SecurityConfig 拦截（ADMIN+）。
-->
<template>
  <div class="admin-file-view">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ==================== 文件管理 ==================== -->
        <el-tab-pane label="文件管理" name="files">
          <div class="filter-bar">
            <el-input
              v-model="filters.username"
              placeholder="用户名/昵称"
              clearable
              class="filter-username"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="filters.userId"
              placeholder="用户 ID（仅可以输入数字）"
              clearable
              class="filter-user"
              @input="onUserIdInput"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="filters.teamId" placeholder="归属" clearable class="filter-ownership">
              <el-option label="个人空间" :value="0" />
              <el-option label="团队空间" :value="-1" />
            </el-select>
            <el-select v-model="filters.category" placeholder="类型" clearable class="filter-category">
              <el-option
                v-for="(code, cat) in FILE_CATEGORY_CODE"
                :key="cat"
                :label="CATEGORY_LABEL[cat]"
                :value="code"
              />
            </el-select>
            <el-select v-model="filters.status" placeholder="状态" clearable class="filter-status">
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="2" />
            </el-select>
            <el-select v-model="filters.sort" class="filter-sort">
              <el-option label="按时间" value="timeDesc" />
              <el-option label="按大小 ↓" value="sizeDesc" />
              <el-option label="按大小 ↑" value="sizeAsc" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>

          <div class="batch-bar">
            <el-button
              :disabled="selectedIds.length === 0"
              type="warning"
              @click="openDisableDialog(selectedRows)"
            >
              批量禁用（{{ selectedIds.length }}）
            </el-button>
            <el-button
              :disabled="selectedIds.length === 0"
              type="success"
              @click="batchStatus('NORMAL')"
            >
              批量启用（{{ selectedIds.length }}）
            </el-button>
            <el-button
              :disabled="selectedIds.length === 0"
              type="danger"
              @click="batchDelete"
            >
              批量删除（{{ selectedIds.length }}）
            </el-button>
            <span class="batch-hint">禁用后用户仍可见文件，但不可下载/预览/分享；删除进入全局回收站</span>
          </div>

          <el-table
            v-loading="loading"
            :data="records"
            @selection-change="(rows: AdminFileItem[]) => (selectedRows = rows)"
            @row-dblclick="openDetail"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column label="名称" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="file-name name-click" @click="preview(row)">
                  <el-icon v-if="row.isDirectory"><Folder /></el-icon>
                  <el-icon v-else class="file-icon"><Document /></el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100">
              <template #default="{ row }">
                {{ row.isDirectory ? '-' : formatBytesAuto(row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="所属" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="owner-cell">
                  <span>{{ row.userName || `用户#${row.userId}` }}</span>
                  <el-tag v-if="row.teamId" size="small" type="info">{{ row.teamName || `团队#${row.teamId}` }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row)" size="small">
                  {{ statusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="165" />
            <el-table-column label="操作" width="300">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="!row.isDirectory" link type="primary" @click="preview(row)">预览</el-button>
                <el-button v-if="!row.isDirectory" link type="primary" @click="download(row)">下载</el-button>
                <el-button v-if="row.status === 'DISABLED'" link type="success" @click="toggleStatus(row, 'NORMAL')">
                  启用
                </el-button>
                <el-button v-else link type="warning" @click="openDisableDialog([row])">禁用</el-button>
                <el-button link type="danger" @click="deleteOne(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="load"
              @size-change="handleSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- ==================== 全局回收站 ==================== -->
        <el-tab-pane label="全局回收站" name="recycle">
          <div class="batch-bar">
            <el-button :disabled="selectedRecycleIds.length === 0" type="danger" @click="batchPurge">
              彻底删除（{{ selectedRecycleIds.length }}）
            </el-button>
            <span class="batch-hint">全局回收站仅管理员可见；恢复按删除来源返回原空间，个人空间校验用户配额</span>
          </div>
          <el-table
            v-loading="recycleLoading"
            :data="recycleItems"
            @selection-change="(rows: AdminRecycleItem[]) => (selectedRecycleRows = rows)"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column label="名称" min-width="240">
              <template #default="{ row }">
                <div class="file-name">
                  <el-icon v-if="row.type === 1"><Folder /></el-icon>
                  <el-icon v-else class="file-icon"><Document /></el-icon>
                  <span>{{ row.originalName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100">
              <template #default="{ row }">
                {{ row.type === 1 ? '-' : formatBytesAuto(row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="所属" min-width="150">
              <template #default="{ row }">
                <div class="owner-cell">
                  <span>{{ row.userName || `用户#${row.userId}` }}</span>
                  <el-tag v-if="row.teamId" size="small" type="info">{{ row.teamName || `团队#${row.teamId}` }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="deletedTime" label="删除时间" width="165" />
            <el-table-column prop="expireTime" label="过期时间" width="165" />
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
                <el-button link type="primary" @click="restore(row)">恢复</el-button>
                <el-button link type="danger" @click="purgeOne(row)">彻底删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!recycleLoading && recycleItems.length === 0" description="全局回收站是空的" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 详情侧栏 -->
    <el-drawer v-model="detailVisible" :title="detail?.name ?? '文件详情'" size="420px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="所属用户">{{ detail.userName || `用户#${detail.userId}` }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.teamId" label="所属团队">
            {{ detail.teamName || `团队#${detail.teamId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ detail.isDirectory ? '目录' : CATEGORY_LABEL[fileCategoryFromCode(detail.category)] }}
          </el-descriptions-item>
          <el-descriptions-item v-if="!detail.isDirectory" label="大小">
            {{ formatBytesAuto(detail.size) }}
          </el-descriptions-item>
          <el-descriptions-item label="路径">{{ detail.path }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail)" size="small">
              {{ statusLabel(detail) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <template v-if="!detail.isDirectory">
            <el-button type="primary" @click="preview(detail)">预览</el-button>
            <el-button @click="download(detail)">下载</el-button>
          </template>
          <el-button
            v-if="detail.status === 'DISABLED'"
            type="success"
            @click="toggleStatus(detail, 'NORMAL')"
          >
            启用
          </el-button>
          <el-button v-else type="warning" @click="openDisableDialog([detail])">禁用</el-button>
          <el-button type="danger" @click="deleteOne(detail)">删除</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 预览（复用用户端预览弹窗，loader 指向管理端预览接口） -->
    <PreviewDialog v-model:visible="previewVisible" :file="previewFileItem" :loader="previewLoader" />

    <!-- 禁用范围（GLOBAL=全站禁 / USER=仅用户） -->
    <el-dialog v-model="disableDialogVisible" title="禁用文件" width="440px">
      <p class="disable-tip">
        禁用后用户仍可见文件，但不可下载/预览/分享；秒传或重新上传相同内容将被拦截。
      </p>
      <el-radio-group v-model="disableScope">
        <el-radio value="USER">仅该用户（只影响文件所属用户）</el-radio>
        <el-radio value="GLOBAL">全站禁（相同内容的所有用户文件一并禁用）</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="disableDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmDisable">确定禁用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Folder } from '@element-plus/icons-vue'
import PreviewDialog from '@/components/file/PreviewDialog.vue'
import {
  adminDownloadUrl,
  adminPreviewFile,
  batchUpdateAdminFileStatus,
  deleteAdminFiles,
  getAdminFileDetail,
  getAdminFiles,
  getAdminRecycleBin,
  purgeAdminRecycle,
  restoreAdminRecycle,
  updateAdminFileStatus,
} from '@/api/admin/file'
import { downloadGet } from '@/utils/request'
import { saveBlob } from '@/utils/download'
import { formatBytesAuto } from '@/utils/format'
import { fileCategoryFromCode, FILE_CATEGORY_CODE } from '@/types/file'
import type { FileItem, FilePreviewResponse } from '@/types/file'
import type { AdminFileItem, AdminRecycleItem, DisableScopeKey } from '@/types/admin'

const CATEGORY_LABEL: Record<string, string> = {
  IMAGE: '图片',
  DOCUMENT: '文档',
  VIDEO: '视频',
  AUDIO: '音频',
  ARCHIVE: '压缩包',
  OTHER: '其他',
}

/** 状态标签：全站禁用=红色，仅用户禁用=黄色，正常=绿色 */
function statusTagType(row: AdminFileItem): string {
  if (row.status !== 'DISABLED') return 'success'
  return row.disabledScope === 'GLOBAL' ? 'danger' : 'warning'
}

function statusLabel(row: AdminFileItem): string {
  if (row.status !== 'DISABLED') return '正常'
  return row.disabledScope === 'GLOBAL' ? '全站禁用' : '已禁用'
}

const activeTab = ref<'files' | 'recycle'>('files')

/* ========== 文件列表 ========== */

const loading = ref(false)
const records = ref<AdminFileItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const selectedRows = ref<AdminFileItem[]>([])
const filters = reactive<{
  username: string
  userId: string
  teamId: number | null
  category: number | null
  status: 1 | 2 | null
  sort: string
}>({ username: '', userId: '', teamId: null, category: null, status: null, sort: 'timeDesc' })

const selectedIds = computed(() => selectedRows.value.map((r) => r.id))

function onUserIdInput(val: string) {
  filters.userId = val.replace(/\D/g, '')
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminFiles({
      username: filters.username || undefined,
      userId: filters.userId ? Number(filters.userId) : undefined,
      teamId: filters.teamId ?? undefined,
      category: filters.category ?? undefined,
      status: filters.status ?? undefined,
      sort: filters.sort as 'timeDesc' | 'sizeDesc' | 'sizeAsc',
      page: page.value,
      size: size.value,
    })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void load()
}

function handleReset() {
  filters.username = ''
  filters.userId = ''
  filters.teamId = null
  filters.category = null
  filters.status = null
  filters.sort = 'timeDesc'
  page.value = 1
  void load()
}

function handleSizeChange() {
  page.value = 1
  void load()
}

/* ========== 状态/删除 ========== */

/* 禁用范围弹窗（GLOBAL=全站禁 / USER=仅用户） */
const disableDialogVisible = ref(false)
const disableRows = ref<AdminFileItem[]>([])
const disableScope = ref<DisableScopeKey>('USER')

function openDisableDialog(rows: AdminFileItem[]) {
  disableRows.value = rows
  disableScope.value = 'USER'
  disableDialogVisible.value = true
}

async function confirmDisable() {
  const ids = disableRows.value.map((r) => r.id)
  if (ids.length === 1) {
    await updateAdminFileStatus(ids[0], 'DISABLED', disableScope.value)
  } else {
    await batchUpdateAdminFileStatus({ ids, status: 'DISABLED', scope: disableScope.value })
  }
  disableDialogVisible.value = false
  ElMessage.success(`已禁用 ${ids.length} 个文件`)
  await load()
  for (const row of disableRows.value) await loadDetailIfOpen(row.id)
}

async function toggleStatus(row: AdminFileItem, status: 'NORMAL' | 'DISABLED') {
  await updateAdminFileStatus(row.id, status)
  ElMessage.success(status === 'DISABLED' ? '已禁用' : '已启用')
  await load()
  await loadDetailIfOpen(row.id)
}

async function batchStatus(status: 'NORMAL' | 'DISABLED') {
  if (selectedIds.value.length === 0) return
  const action = status === 'DISABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${action}选中的 ${selectedIds.value.length} 个文件吗？禁用后用户不可下载/预览/分享。`,
      `批量${action}`,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 'DISABLED' ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  await batchUpdateAdminFileStatus({ ids: selectedIds.value, status })
  ElMessage.success(`已${action} ${selectedIds.value.length} 个文件`)
  await load()
}

async function deleteOne(row: AdminFileItem) {
  await confirmDelete([row])
}

async function batchDelete() {
  await confirmDelete(selectedRows.value)
}

async function confirmDelete(rows: AdminFileItem[]) {
  if (rows.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除 ${rows.length} 个文件/目录吗？删除进入全局回收站，可恢复。`,
      '删除文件',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await deleteAdminFiles(rows.map((r) => r.id))
  ElMessage.success('已移入全局回收站')
  await load()
  await loadRecycle()
}

/* ========== 详情 ========== */

const detailVisible = ref(false)
const detail = ref<AdminFileItem | null>(null)

async function openDetail(row: AdminFileItem) {
  detail.value = await getAdminFileDetail(row.id)
  detailVisible.value = true
}

async function loadDetailIfOpen(id: number) {
  if (detailVisible.value && detail.value?.id === id) {
    detail.value = await getAdminFileDetail(id)
  }
}

/* ========== 预览 / 下载 ========== */

const previewVisible = ref(false)
const previewFileItem = ref<FileItem | null>(null)

function preview(row: AdminFileItem) {
  previewFileItem.value = row as unknown as FileItem
  previewVisible.value = true
}

async function previewLoader(file: FileItem): Promise<FilePreviewResponse> {
  return adminPreviewFile(file.id)
}

async function download(row: AdminFileItem) {
  try {
    const blob = await downloadGet(adminDownloadUrl(row.id))
    saveBlob(blob, row.name)
  } catch {
    // 拦截器已提示错误
  }
}

/* ========== 全局回收站 ========== */

const recycleLoading = ref(false)
const recycleItems = ref<AdminRecycleItem[]>([])
const selectedRecycleRows = ref<AdminRecycleItem[]>([])
const selectedRecycleIds = computed(() => selectedRecycleRows.value.map((r) => r.id))

async function loadRecycle() {
  recycleLoading.value = true
  try {
    recycleItems.value = await getAdminRecycleBin()
  } finally {
    recycleLoading.value = false
  }
}

async function restore(row: AdminRecycleItem) {
  try {
    await ElMessageBox.confirm(
      `确定恢复「${row.originalName}」吗？将返回原空间（个人空间校验用户配额）。`,
      '恢复文件',
      { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  await restoreAdminRecycle(row.id)
  ElMessage.success('已恢复')
  await loadRecycle()
  await load()
}

async function purgeOne(row: AdminRecycleItem) {
  await confirmPurge([row])
}

async function batchPurge() {
  await confirmPurge(selectedRecycleRows.value)
}

async function confirmPurge(rows: AdminRecycleItem[]) {
  if (rows.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定彻底删除 ${rows.length} 条回收站记录吗？物理数据将不可恢复。`,
      '彻底删除',
      { confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'error' },
    )
  } catch {
    return
  }
  await purgeAdminRecycle(rows.map((r) => r.id))
  ElMessage.success('已彻底删除')
  await loadRecycle()
}

/* ========== 挂载 ========== */

onMounted(() => {
  void load()
  void loadRecycle()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-user {
  width: 180px;
}

.filter-username {
  width: 150px;
}

.disable-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.filter-ownership {
  width: 130px;
}

.filter-category {
  width: 120px;
}

.filter-status {
  width: 100px;
}

.filter-sort {
  width: 120px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.batch-hint {
  font-size: 12px;
  color: #909399;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name-click {
  cursor: pointer;
}

.name-click:hover {
  color: #409eff;
}

.file-icon {
  color: #409eff;
}

.owner-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
</style>
