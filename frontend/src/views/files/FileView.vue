<!--
  FileView —— 文件管理主页面（/files）。
  布局：左侧目录树 + 右侧列表；顶部工具栏（上传/新建文件夹/批量下载/传输队列）+ 搜索。
  功能范围：目录浏览、创建目录、上传（分片/秒传）、下载/打包下载、
  重命名/移动/复制、删除（回收站）、搜索、预览。
-->
<template>
  <div class="file-view">
    <div class="file-header">
      <BreadcrumbNav v-if="!fileStore.isSearching" />
      <span v-else class="search-title">搜索：「{{ fileStore.keyword }}」</span>

      <div class="search-bar">
        <el-select
          v-model="category"
          placeholder="类型"
          clearable
          style="width: 110px"
          @change="handleSearch"
        >
          <el-option v-for="cat in CATEGORY_OPTIONS" :key="cat.value" :label="cat.label" :value="cat.value" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索文件名"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button v-if="fileStore.isSearching" @click="handleClearSearch">返回目录</el-button>
      </div>
    </div>

    <div class="file-body">
      <!-- 左侧目录树 -->
      <el-card shadow="never" class="tree-card">
        <DirectoryTree />
      </el-card>

      <!-- 右侧列表 -->
      <el-card shadow="never" class="list-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" @click="uploadDialogVisible = true">
              <el-icon><Upload /></el-icon>
              <span>上传文件</span>
            </el-button>
            <el-button @click="handleCreateDirectory">
              <el-icon><FolderAdd /></el-icon>
              <span>新建文件夹</span>
            </el-button>
            <el-button :disabled="!fileStore.hasSelection" @click="handleBatchDownload">
              <el-icon><Download /></el-icon>
              <span>打包下载</span>
            </el-button>
          </div>
          <div class="toolbar-right">
            <el-button @click="router.push('/recycle-bin')">
              <el-icon><Delete /></el-icon>
              <span>回收站</span>
            </el-button>
            <el-badge :value="uploadStore.activeCount" :hidden="uploadStore.activeCount === 0">
              <el-button @click="queueRef?.open()">
                <el-icon><List /></el-icon>
                <span>传输队列</span>
              </el-button>
            </el-badge>
          </div>
        </div>

        <FileList
          @preview="handlePreview"
          @move-copy="handleMoveCopy"
          @batch-move-copy="handleBatchMoveCopy"
          @share="handleShare"
        />
      </el-card>
    </div>

    <!-- 对话框 -->
    <UploadDialog v-model:visible="uploadDialogVisible" :parent-id="fileStore.currentDirId" />
    <PreviewDialog v-model:visible="previewVisible" :file="previewFile" />
    <MoveCopyDialog v-model:visible="moveCopyVisible" :targets="moveCopyTargets" :mode="moveCopyMode" />
    <ShareCreateDialog v-model:visible="shareDialogVisible" :file-id="shareTargetId" @created="handleShareCreated" />
    <TransferQueue ref="queueRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, FolderAdd, Search, List, Delete } from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import { useUploadStore } from '@/stores/upload'
import { createDirectory } from '@/api/file'
import BreadcrumbNav from '@/components/file/BreadcrumbNav.vue'
import DirectoryTree from '@/components/file/DirectoryTree.vue'
import FileList from '@/components/file/FileList.vue'
import UploadDialog from '@/components/file/UploadDialog.vue'
import PreviewDialog from '@/components/file/PreviewDialog.vue'
import MoveCopyDialog from '@/components/file/MoveCopyDialog.vue'
import TransferQueue from '@/components/file/TransferQueue.vue'
import ShareCreateDialog from '@/components/share/ShareCreateDialog.vue'
import type { FileCategory, FileItem } from '@/types/file'
import type { ShareItem } from '@/types/share'

const fileStore = useFileStore()
const uploadStore = useUploadStore()
const router = useRouter()

/* ========== 工具栏 ========== */

const uploadDialogVisible = ref(false)
const queueRef = ref<InstanceType<typeof TransferQueue> | null>(null)

async function handleCreateDirectory() {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (v.trim() ? true : '名称不能为空'),
    })
    await createDirectory({ parentId: fileStore.currentDirId, name: value.trim() })
    ElMessage.success('创建成功')
    fileStore.refresh()
    fileStore.loadTree(true)
  } catch {
    // 用户取消或错误已提示
  }
}

async function handleBatchDownload() {
  const selected = fileStore.selectedFiles
  if (selected.length === 0) return
  const name =
    selected.length === 1 ? selected[0].name : `打包下载（${selected.length} 个文件）`
  await uploadStore.createBatchDownload(
    selected.map((f) => f.id),
    name,
  )
  ElMessage.info('已创建打包任务，完成前可在传输队列查看进度')
}

/* ========== 搜索 ========== */

const keyword = ref('')
const category = ref<FileCategory | undefined>(undefined)

/** 搜索类型过滤选项 —— 文件名 + 类型过滤 */
const CATEGORY_OPTIONS: Array<{ value: FileCategory; label: string }> = [
  { value: 'IMAGE', label: '图片' },
  { value: 'VIDEO', label: '视频' },
  { value: 'AUDIO', label: '音频' },
  { value: 'DOCUMENT', label: '文档' },
  { value: 'ARCHIVE', label: '压缩包' },
]

async function handleSearch() {
  await fileStore.startSearch(keyword.value, category.value)
}

async function handleClearSearch() {
  keyword.value = ''
  category.value = undefined
  await fileStore.clearSearch()
}

/* ========== 预览 / 移动复制 ========== */

const previewVisible = ref(false)
const previewFile = ref<FileItem | null>(null)

function handlePreview(file: FileItem) {
  previewFile.value = file
  previewVisible.value = true
}

const moveCopyVisible = ref(false)
const moveCopyTargets = ref<FileItem[]>([])
const moveCopyMode = ref<'move' | 'copy'>('move')

function handleMoveCopy(file: FileItem, mode: 'move' | 'copy') {
  moveCopyTargets.value = [file]
  moveCopyMode.value = mode
  moveCopyVisible.value = true
}

/** 批量移动/复制（列表多选触发） */
function handleBatchMoveCopy(files: FileItem[], mode: 'move' | 'copy') {
  moveCopyTargets.value = files
  moveCopyMode.value = mode
  moveCopyVisible.value = true
}

/* ========== 分享 ========== */

const shareDialogVisible = ref(false)
const shareTargetId = ref(0)

function handleShare(file: FileItem) {
  shareTargetId.value = file.id
  shareDialogVisible.value = true
}

function handleShareCreated(share: ShareItem) {
  const link = `${location.origin}${location.pathname}#/s/${share.shareToken}`
  navigator.clipboard?.writeText(link).catch(() => {})
  ElMessageBox.alert(
    `分享链接（已复制到剪贴板）：\n${link}\n\n提取码：${share.requirePassword ? '已设置（创建时填写的密码）' : '无'}`,
    '分享成功',
    { confirmButtonText: '知道了' },
  ).catch(() => {})
}

/* ========== 初始化 ========== */

onMounted(async () => {
  uploadStore.init()
  await Promise.all([fileStore.load(), fileStore.loadTree()])
})
</script>

<style scoped>
.file-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0;
}

.search-title {
  font-size: 14px;
  color: #606266;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-body {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.tree-card {
  width: 240px;
  flex-shrink: 0;
}

.list-card {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
