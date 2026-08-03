<!--
  FileList —— 文件列表（列表/图标双视图）。
  数据与选中项来自 fileStore；操作按钮显隐由 can() 规则表 + 后端能力字段共同推导。
  重命名/删除在组件内完成（弹确认框），预览/移动/复制上抛给父组件打开对话框。
-->
<template>
  <div class="file-list">
    <!-- 批量操作条：多选后出现 -->
    <div v-if="fileStore.selectedFiles.length > 0" class="batch-bar">
      <span class="batch-count">已选 {{ fileStore.selectedFiles.length }} 项</span>
      <el-button size="small" @click="handleBatchDownload">下载</el-button>
      <el-button size="small" @click="handleBatchMoveCopy('move')">移动</el-button>
      <el-button size="small" @click="handleBatchMoveCopy('copy')">复制</el-button>
      <el-button size="small" type="danger" plain @click="handleBatchDelete">删除</el-button>
      <el-button size="small" link type="info" @click="handleClearSelection">取消选择</el-button>
    </div>

    <!-- 列表视图 -->
    <el-table
      v-if="viewMode === 'list'"
      :data="fileStore.fileList"
      v-loading="fileStore.loading"
      row-key="id"
      stripe
      size="default"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column label="名称" min-width="260">
        <template #default="{ row }">
          <div class="file-name" @click="handleNameClick(row)">
            <el-icon :size="18" :class="fileIconClass(row)">
              <component :is="fileIcon(row)" />
            </el-icon>
            <span
              class="file-name-text"
              :class="{ 'name-clickable': isNameClickable(row) }"
            >{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">
          {{ row.type === 'FILE' ? formatBytesAuto(row.size) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="170">
        <template #default="{ row }">
          {{ row.updatedAt?.slice(0, 16).replace('T', ' ') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="fileCan('rename', row)"
            link
            type="primary"
            size="small"
            @click="handleRename(row)"
          >
            重命名
          </el-button>
          <el-dropdown
            trigger="click"
            class="more-dropdown"
            @command="(cmd: string) => handleMore(row, cmd)"
          >
            <el-button link type="primary" size="small">
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="fileCan('download', row)" command="download">下载</el-dropdown-item>
                <el-dropdown-item v-if="fileCan('share', row)" command="share">分享</el-dropdown-item>
                <el-dropdown-item v-if="fileCan('move', row)" command="move">移动</el-dropdown-item>
                <el-dropdown-item v-if="fileCan('copy', row)" command="copy">复制</el-dropdown-item>
                <el-dropdown-item v-if="fileCan('delete', row)" command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无文件" :image-size="80" />
      </template>
    </el-table>

    <!-- 图标视图 -->
    <div v-else v-loading="fileStore.loading" class="icon-grid">
      <div
        v-for="item in fileStore.fileList"
        :key="item.id"
        class="icon-card"
        :class="{ selected: isSelected(item) }"
        @click="handleIconClick(item)"
        @dblclick="handleRowDblClick(item)"
      >
        <div class="icon-thumb">
          <el-icon :size="40" :class="fileIconClass(item)">
            <component :is="fileIcon(item)" />
          </el-icon>
        </div>
        <div class="icon-name" :title="item.name">{{ item.name }}</div>
      </div>
      <el-empty v-if="fileStore.fileList.length === 0" description="暂无文件" :image-size="80" />
    </div>

    <!-- 分页 -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="fileStore.page"
        v-model:page-size="fileStore.pageSize"
        :total="fileStore.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="handlePageChange"
        @size-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Document,
  Folder,
  Files,
  Picture,
  VideoCamera,
  Microphone,
} from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import { useUserStore } from '@/stores/user'
import { can } from '@/utils/permission'
import { FILE_OPERATIONS } from '@/permissions/file-operations'
import { renameFile, deleteFile, downloadFile } from '@/api/file'
import { formatBytesAuto } from '@/utils/format'
import {
  fileCategory,
  isPreviewable,
  type FileItem,
  type FileCategory,
} from '@/types/file'

const emit = defineEmits<{
  preview: [file: FileItem]
  'move-copy': [file: FileItem, mode: 'move' | 'copy']
  'batch-move-copy': [files: FileItem[], mode: 'move' | 'copy']
  share: [file: FileItem]
}>()

const fileStore = useFileStore()
const userStore = useUserStore()

/** 视图模式：列表 / 图标 */
const viewMode = ref<'list' | 'icon'>('list')

/** 图标视图单选标记（点击选中，双击打开） */
const iconSelected = ref<number | null>(null)

/* ========== 权限推导 ========== */

/**
 * 文件操作显隐：can() 规则表（角色 + 文件类型）推导。
 * 个人空间无协作角色差异，全员可操作；
 * 资源级能力由后端按需校验（如回收站/父目录状态），前端不重复维护。
 */
function fileCan(operation: string, row: FileItem): boolean {
  return can(operation, userStore.role, row.type, FILE_OPERATIONS)
}

/* ========== 图标与样式（前端维护的 UI 映射） ========== */

const CATEGORY_ICON: Record<string, unknown> = {
  IMAGE: Picture,
  VIDEO: VideoCamera,
  AUDIO: Microphone,
  DOCUMENT: Document,
  ARCHIVE: Files,
  OTHER: Document,
}

const CATEGORY_COLOR: Record<string, string> = {
  IMAGE: '#67c23a',
  VIDEO: '#f56c6c',
  AUDIO: '#e6a23c',
  DOCUMENT: '#409eff',
  ARCHIVE: '#909399',
  OTHER: '#909399',
}

function fileIcon(row: FileItem): unknown {
  if (row.type === 'DIRECTORY') return Folder
  const category = fileCategory(row.name, row.type) ?? 'OTHER'
  return CATEGORY_ICON[category]
}

function fileIconClass(row: FileItem): string {
  if (row.type === 'DIRECTORY') return 'icon-dir'
  const category: FileCategory = fileCategory(row.name, row.type) ?? 'OTHER'
  return `icon-${category.toLowerCase()}`
}

/* ========== 交互 ========== */

function handleSelectionChange(rows: FileItem[]) {
  fileStore.selectedFiles = rows
}

function isSelected(item: FileItem): boolean {
  return iconSelected.value === item.id
}

function handleIconClick(item: FileItem) {
  iconSelected.value = iconSelected.value === item.id ? null : item.id
}

/** 双击：目录进入，文件预览（可预览时）。已由单击名称承载，保留以兼容图标视图双击 */
function handleRowDblClick(row: FileItem) {
  handleNameClick(row)
}

/** 名称是否可点击：目录（进入）或可预览文件 */
function isNameClickable(row: FileItem): boolean {
  if (row.type === 'DIRECTORY') return true
  return fileCan('preview', row) && isPreviewable(row.name, row.type)
}

/** 单击名称：目录进入，可预览文件打开预览 */
function handleNameClick(row: FileItem) {
  if (row.type === 'DIRECTORY') {
    fileStore.navigate(row.id)
    return
  }
  if (fileCan('preview', row) && isPreviewable(row.name, row.type)) {
    emit('preview', row)
  }
}

async function handleDownload(row: FileItem) {
  try {
    await downloadFile(row)
    ElMessage.success('下载已开始')
  } catch {
    // 错误已在拦截器中提示
  }
}

/** 操作列"更多"菜单分发 */
function handleMore(row: FileItem, command: string) {
  if (command === 'download') {
    handleDownload(row)
  } else if (command === 'share') {
    emit('share', row)
  } else if (command === 'move' || command === 'copy') {
    emit('move-copy', row, command)
  } else if (command === 'delete') {
    handleDelete(row)
  }
}

/* ========== 批量操作 ========== */

/** 批量下载（逐个文件下载，区别于打包下载） */
async function handleBatchDownload() {
  const selected = fileStore.selectedFiles.filter((f) => f.type === 'FILE')
  if (selected.length === 0) {
    ElMessage.warning('请先选择要下载的文件')
    return
  }
  try {
    await Promise.allSettled(selected.map((f) => downloadFile(f)))
    ElMessage.success(`已开始下载 ${selected.length} 个文件`)
  } catch {
    // 错误已在拦截器中提示
  }
}

/** 批量移动/复制 */
function handleBatchMoveCopy(mode: 'move' | 'copy') {
  const selected = fileStore.selectedFiles
  if (selected.length === 0) return
  emit('batch-move-copy', selected, mode)
}

/** 批量删除（移入回收站） */
async function handleBatchDelete() {
  const selected = fileStore.selectedFiles
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selected.length} 项移入回收站吗？目录会连同其中所有文件一并移入。`,
      '批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await Promise.all(selected.map((f) => deleteFile(f.id)))
    ElMessage.success('已移入回收站')
    fileStore.clearSelection()
    fileStore.refresh()
  } catch {
    // 错误已在拦截器中提示
  }
}

function handleClearSelection() {
  fileStore.clearSelection()
}

async function handleRename(row: FileItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名', {
      inputValue: row.name,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (v.trim() ? true : '名称不能为空'),
    })
    await renameFile(row.id, { name: value.trim() })
    ElMessage.success('重命名成功')
    fileStore.refresh()
  } catch {
    // 用户取消或错误已提示
  }
}

async function handleDelete(row: FileItem) {
  const isDir = row.type === 'DIRECTORY'
  try {
    await ElMessageBox.confirm(
      isDir ? `确定将目录「${row.name}」及其中所有文件移入回收站吗？` : `确定将「${row.name}」移入回收站吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await deleteFile(row.id)
    ElMessage.success('已移入回收站')
    fileStore.refresh()
  } catch {
    // 错误已在拦截器中提示
  }
}

async function handlePageChange() {
  await fileStore.load()
}
</script>

<style scoped>
.file-list {
  position: relative;
}

/* 批量操作条 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #ecf5ff;
  border-radius: 4px;
}

/* 操作列"更多"下拉：el-dropdown 包一层 span 导致比相邻按钮高，这里拉齐基线 */
.more-dropdown {
  vertical-align: middle;
}

.more-dropdown :deep(.el-dropdown__button) {
  vertical-align: middle;
}

.batch-count {
  font-size: 13px;
  color: #409eff;
  margin-right: 8px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-clickable {
  cursor: pointer;
  transition: color 0.2s;
}

.name-clickable:hover {
  color: #409eff;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  padding: 12px 0;
  min-height: 300px;
}

.icon-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.icon-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.icon-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
}

.icon-name {
  width: 100%;
  text-align: center;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-dir {
  color: #e6a23c;
}

.icon-image {
  color: #67c23a;
}

.icon-video {
  color: #f56c6c;
}

.icon-audio {
  color: #e6a23c;
}

.icon-document {
  color: #409eff;
}

.icon-archive {
  color: #909399;
}

.icon-other {
  color: #909399;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
