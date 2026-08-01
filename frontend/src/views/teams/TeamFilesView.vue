<!--
  TeamFilesView —— 团队文件页面（/teams/:id/files）。
  团队命名空间（同团队同目录 name 唯一）；上传/秒传复用个人上传链路（请求体带 teamId）。
  权限矩阵（docs/team-module.md §三）：MEMBER 只能操作自己上传的文件，ADMIN/OWNER 可操作全部；
  下载/预览所有成员可。
-->
<template>
  <div class="team-files-view">
    <div class="tf-header">
      <el-page-header @back="router.push('/teams')">
        <template #title>
          <span class="tf-title">{{ team?.name ?? '团队' }} 文件</span>
        </template>
      </el-page-header>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item @click="navTo(0)" :class="{ clickable: pathStack.length > 0 }">
          全部文件
        </el-breadcrumb-item>
        <el-breadcrumb-item v-for="(d, i) in pathStack" :key="d.id" @click="navTo(d.id, i + 1)">
          {{ d.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="uploadVisible = true">
            <el-icon><Upload /></el-icon>
            <span>上传文件</span>
          </el-button>
          <el-button @click="handleCreateDirectory">
            <el-icon><FolderAdd /></el-icon>
            <span>新建文件夹</span>
          </el-button>
          <el-button @click="router.push(`/teams/${teamId}/recycle`)">
            <el-icon><Delete /></el-icon>
            <span>团队回收站</span>
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-tag v-if="team" type="info" effect="plain">
            已用 {{ formatBytesAuto(team.usedSpace) }} / {{ formatBytesAuto(team.quota) }}
          </el-tag>
          <el-badge :value="uploadStore.activeCount" :hidden="uploadStore.activeCount === 0">
            <el-button @click="queueRef?.open()">
              <el-icon><List /></el-icon>
              <span>传输队列</span>
            </el-button>
          </el-badge>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="files"
        row-key="id"
        @row-dblclick="handleRowDoubleClick"
      >
        <el-table-column label="名称" min-width="240">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon v-if="row.isDirectory"><Folder /></el-icon>
              <el-icon v-else class="file-icon"><Document /></el-icon>
              <span class="name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ row.isDirectory ? '-' : formatBytesAuto(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-if="row.isDirectory" link type="primary" @click="openDir(row)">打开</el-button>
            <template v-else>
              <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
              <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            </template>
            <el-button link type="primary" @click="handleRename(row)">重命名</el-button>
            <el-button link type="primary" @click="handleMoveCopy(row, 'move')">移动</el-button>
            <el-button link type="primary" @click="handleMoveCopy(row, 'copy')">复制</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 上传（复用个人上传链路，请求体带 teamId） -->
    <el-dialog v-model="uploadVisible" title="上传文件到团队" width="440px" draggable>
      <el-upload
        drag
        multiple
        :auto-upload="false"
        v-model:file-list="selectedList"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedList.length === 0" @click="handleUpload">
          开始上传（{{ selectedList.length }}）
        </el-button>
      </template>
    </el-dialog>

    <PreviewDialog
      v-model:visible="previewVisible"
      :file="previewTarget"
      :loader="previewLoader"
    />
    <MoveCopyTeamDialog
      v-model:visible="moveCopyVisible"
      :team-id="teamId"
      :target="moveCopyTarget"
      :mode="moveCopyMode"
      :exclude-id="moveCopyExcludeId"
      @done="load"
    />
    <TransferQueue ref="queueRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Document,
  Folder,
  FolderAdd,
  List,
  Upload,
  UploadFilled,
} from '@element-plus/icons-vue'
import { useUploadStore } from '@/stores/upload'
import {
  createTeamDirectory,
  deleteTeamFile,
  downloadTeamFile,
  getTeamDetail,
  getTeamFileList,
  previewTeamFile,
  renameTeamFile,
} from '@/api/team'
import PreviewDialog from '@/components/file/PreviewDialog.vue'
import TransferQueue from '@/components/file/TransferQueue.vue'
import MoveCopyTeamDialog from '@/components/team/MoveCopyTeamDialog.vue'
import type { UploadUserFile } from 'element-plus'
import { formatBytesAuto } from '@/utils/format'
import type { FileItem, FilePreviewResponse } from '@/types/file'
import type { Team } from '@/types/team'

const route = useRoute()
const router = useRouter()
const uploadStore = useUploadStore()

const teamId = computed(() => Number(route.params.id))
const team = ref<Team | null>(null)
const files = ref<FileItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

/* 面包屑路径栈：{id, name}，根目录为 [] */
const pathStack = ref<Array<{ id: number; name: string }>>([])
const currentDirId = computed(() => pathStack.value[pathStack.value.length - 1]?.id ?? 0)

async function load() {
  loading.value = true
  try {
    const res = await getTeamFileList(teamId.value, currentDirId.value, page.value, size.value)
    files.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openDir(dir: FileItem) {
  pathStack.value.push({ id: dir.id, name: dir.name })
  page.value = 1
  void load()
}

function navTo(id: number, depth?: number) {
  if (depth !== undefined) {
    pathStack.value = pathStack.value.slice(0, depth)
  } else {
    pathStack.value = []
  }
  page.value = 1
  void load()
}

function handleRowDoubleClick(row: FileItem) {
  if (row.isDirectory) openDir(row)
}

async function handleCreateDirectory() {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (v.trim() ? true : '名称不能为空'),
    })
    await createTeamDirectory(teamId.value, { parentId: currentDirId.value, name: value.trim() })
    ElMessage.success('创建成功')
    await load()
  } catch {
    // 用户取消或错误已提示
  }
}

async function handleRename(file: FileItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名', {
      inputValue: file.name,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (v.trim() ? true : '名称不能为空'),
    })
    await renameTeamFile(teamId.value, file.id, { name: value.trim() })
    ElMessage.success('重命名成功')
    await load()
  } catch {
    // 取消或错误已提示
  }
}

async function handleDelete(file: FileItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${file.name}」吗？将移入团队回收站（${teamRecycleDays} 天后物理清除）。`,
      '删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await deleteTeamFile(teamId.value, file.id)
  ElMessage.success('已移入团队回收站')
  await load()
}

const teamRecycleDays = 30

async function handleDownload(file: FileItem) {
  await downloadTeamFile(teamId.value, file)
}

/* ========== 预览 ========== */

const previewVisible = ref(false)
const previewTarget = ref<FileItem | null>(null)

const previewLoader = computed(() => (file: FileItem) => previewTeamFile(teamId.value, file.id))

function handlePreview(file: FileItem) {
  previewTarget.value = file
  previewVisible.value = true
}

/* ========== 移动 / 复制 ========== */

const moveCopyVisible = ref(false)
const moveCopyTarget = ref<FileItem | null>(null)
const moveCopyMode = ref<'move' | 'copy'>('move')
const moveCopyExcludeId = ref<number | null>(null)

function handleMoveCopy(file: FileItem, mode: 'move' | 'copy') {
  moveCopyTarget.value = file
  moveCopyMode.value = mode
  moveCopyExcludeId.value = file.id
  moveCopyVisible.value = true
}

/* ========== 上传 ========== */

const uploadVisible = ref(false)
const selectedList = ref<UploadUserFile[]>([])

function handleUpload() {
  const raws = selectedList.value.flatMap((f) => (f.raw ? [f.raw] : []))
  if (raws.length === 0) return
  void uploadStore.uploadFiles(raws, currentDirId.value, teamId.value)
  uploadVisible.value = false
  selectedList.value = []
  ElMessage.success('已加入传输队列')
}

const queueRef = ref<InstanceType<typeof TransferQueue> | null>(null)

onMounted(() => {
  uploadStore.init()
  void getTeamDetail(teamId.value).then((t) => {
    team.value = t
  })
  void load()
})
</script>

<style scoped>
.team-files-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tf-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0;
}

.tf-title {
  font-size: 15px;
  font-weight: 600;
}

.clickable {
  cursor: pointer;
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

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.file-icon {
  color: #409eff;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
