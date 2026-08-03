<!--
  GuestShareView —— 访客分享访问页（/s/:token，无登录要求）。
  流程：加载分享信息 → 需提取码则先验证 → 浏览快照树（平铺节点按 parentId 组织）
  → 预览 / 单文件下载 / 批量打包下载（轮询任务）/ 转存（需登录）。
-->
<template>
  <div class="guest-share">
    <div v-if="pageState === 'loading'" v-loading="true" class="state-wrap">
      <el-empty description="加载中..." :image-size="60" />
    </div>

    <!-- 不可访问：过期/取消/用尽/不存在/异常 -->
    <div v-else-if="pageState === 'error'" class="state-wrap">
      <el-empty :description="errorMessage" :image-size="80">
        <el-button type="primary" @click="router.push('/files')">去云盘看看</el-button>
      </el-empty>
    </div>

    <!-- 需提取码 -->
    <div v-else-if="pageState === 'password'" class="state-wrap">
      <el-card class="pwd-card">
        <h3 class="pwd-title">🔒 {{ info?.name || '文件分享' }}</h3>
        <p class="pwd-sub">分享者：{{ info?.ownerName }} · 该分享需要提取码</p>
        <el-input
          v-model="passwordInput"
          placeholder="请输入提取码"
          maxlength="64"
          show-password
          class="pwd-input"
          @keyup.enter="handleVerify"
        />
        <el-button type="primary" class="pwd-btn" :loading="verifying" @click="handleVerify">验证</el-button>
      </el-card>
    </div>

    <!-- 已授权：浏览 / 下载 / 转存 -->
    <div v-else class="guest-body">
      <div class="guest-header">
        <div class="guest-title">
          <el-icon :size="20" :color="info?.isDir ? '#e6a23c' : '#409eff'">
            <Folder v-if="info?.isDir" />
            <Document v-else />
          </el-icon>
          <span class="title-text">{{ info?.name }}</span>
        </div>
        <div class="guest-meta">
          <span>分享者：{{ info?.ownerName }}</span>
          <span v-if="info?.downloadCount !== undefined && info?.maxDownload !== undefined">
            下载 {{ info.downloadCount }}<template v-if="info.maxDownload > 0"> / {{ info.maxDownload }}</template> 次
          </span>
          <span v-if="!info?.allowDownload" class="deny-tag">仅可预览</span>
          <el-button size="small" @click="router.push('/files')">返回云盘</el-button>
        </div>
      </div>

      <!-- 批量操作条 -->
      <div v-if="selectedIds.size > 0" class="batch-bar">
        <span class="batch-count">已选 {{ selectedIds.size }} 项</span>
        <el-button v-if="info?.allowDownload" size="small" type="primary" @click="handleBatchDownload">
          打包下载
        </el-button>
        <el-button v-if="info?.allowSave" size="small" :loading="saving" @click="handleSave">
          转存到我网盘
        </el-button>
        <el-button size="small" link type="info" @click="selectedIds.clear()">取消选择</el-button>
      </div>

      <!-- 面包屑 -->
      <div class="breadcrumb">
        <el-link
          v-for="(crumb, index) in crumbs"
          :key="crumb.id"
          type="primary"
          :underline="false"
          class="crumb"
          @click="enterDir(crumb.id, index)"
        >
          {{ crumb.name }}
        </el-link>
      </div>

      <!-- 文件列表 -->
      <el-table v-loading="loadingFiles" :data="currentNodes" stripe style="width: 100%">
        <el-table-column type="selection" width="45" @selection-change="handleSelectionChange" />
        <el-table-column label="名称" min-width="260">
          <template #default="{ row }">
            <div class="node-name" @click="handleNodeClick(row)">
              <el-icon :size="18" :color="row.isDir ? '#e6a23c' : '#409eff'">
                <Folder v-if="row.isDir" />
                <Document v-else />
              </el-icon>
              <span class="node-name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ row.isDir ? '—' : formatBytesAuto(row.size) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="!row.isDir">
              <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
              <el-button
                v-if="info?.allowDownload"
                link
                type="primary"
                size="small"
                @click="handleDownloadOne(row)"
              >下载</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="此目录为空" :image-size="80" />
        </template>
      </el-table>

      <!-- 预览对话框 -->
      <el-dialog v-model="previewVisible" :title="previewing?.name ?? '预览'" width="760px" top="6vh">
        <div v-loading="previewLoading" class="preview-body">
          <img v-if="preview?.type === 'IMAGE'" :src="preview.thumbnailUrl || preview.url || ''" class="preview-media" alt="预览" />
          <video v-else-if="preview?.type === 'VIDEO'" :src="preview.url ?? ''" controls class="preview-media" />
          <audio v-else-if="preview?.type === 'AUDIO'" :src="preview.url ?? ''" controls class="preview-audio" />
          <iframe v-else-if="preview?.type === 'PDF'" :src="preview.url ?? ''" class="preview-pdf" />
          <pre v-else-if="preview?.type === 'TEXT'" class="preview-text">{{ preview.content }}</pre>
          <el-empty v-else-if="!previewLoading" description="该文件类型暂不支持预览" :image-size="80" />
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import {
  batchDownloadShare,
  downloadShareFile,
  getGuestShareInfo,
  getShareFiles,
  previewShareFile,
  queryShareBatchTask,
  saveShareFiles,
  verifySharePassword,
} from '@/api/share'
import { downloadByUrl } from '@/utils/download'
import { formatBytesAuto } from '@/utils/format'
import type { GuestShareInfo, ShareFileNode } from '@/types/share'

const route = useRoute()
const router = useRouter()

const token = computed(() => String(route.params.token ?? ''))

const pageState = ref<'loading' | 'error' | 'password' | 'ready'>('loading')
const errorMessage = ref('')
const info = ref<GuestShareInfo | null>(null)

const passwordInput = ref('')
const verifying = ref(false)

const nodes = ref<ShareFileNode[]>([])
const loadingFiles = ref(false)
/** 当前浏览路径（快照 id 栈，根为 0） */
const pathStack = ref<number[]>([0])
const selectedIds = ref(new Set<number>())
const saving = ref(false)

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewing = ref<ShareFileNode | null>(null)
const preview = ref<{ type: string; url: string | null; thumbnailUrl: string | null; content: string | null } | null>(null)

/** 平铺快照 → 子节点映射 */
const childrenByParent = computed(() => {
  const map = new Map<number, ShareFileNode[]>()
  for (const node of nodes.value) {
    const list = map.get(node.parentId) ?? []
    list.push(node)
    map.set(node.parentId, list)
  }
  return map
})

/** 当前目录节点 */
const currentNodes = computed(() => childrenByParent.value.get(pathStack.value[pathStack.value.length - 1]) ?? [])

const crumbs = computed(() => {
  const crumbs = [{ id: 0, name: info.value?.name ?? '分享' }]
  for (const id of pathStack.value.slice(1)) {
    const node = nodes.value.find((n) => n.id === id)
    if (node) crumbs.push({ id: node.id, name: node.name })
  }
  return crumbs
})

/* ========== 加载 ========== */

async function load() {
  pageState.value = 'loading'
  try {
    info.value = await getGuestShareInfo(token.value)
    if (info.value.status !== 'NORMAL') {
      pageState.value = 'error'
      errorMessage.value = {
        EXPIRED: '分享已过期',
        CANCELED: '分享已取消',
        EXHAUSTED: '分享下载次数已用尽',
      }[info.value.status] ?? '分享不可用'
      return
    }
    pageState.value = info.value.requirePassword ? 'password' : 'ready'
    if (pageState.value === 'ready') {
      await loadFiles()
    }
  } catch (err) {
    const e = err as { code?: number }
    if (e.code === 10303) errorMessage.value = '分享不存在'
    else if (e.code === 10300) errorMessage.value = '分享已过期'
    else if (e.code === 10306) errorMessage.value = '分享已取消'
    else errorMessage.value = '分享加载失败'
    pageState.value = 'error'
  }
}

async function loadFiles() {
  loadingFiles.value = true
  try {
    nodes.value = await getShareFiles(token.value)
  } catch {
    // 错误已由拦截器提示
  } finally {
    loadingFiles.value = false
  }
}

/* ========== 提取码 ========== */

async function handleVerify() {
  if (!passwordInput.value.trim()) {
    ElMessage.warning('请输入提取码')
    return
  }
  verifying.value = true
  try {
    await verifySharePassword(token.value, passwordInput.value.trim())
    pageState.value = 'ready'
    await loadFiles()
  } catch (err) {
    const e = err as { code?: number }
    if (e.code === 10307) {
      pageState.value = 'error'
      errorMessage.value = '提取码错误次数过多，请重新打开链接'
    }
  } finally {
    verifying.value = false
  }
}

/* ========== 浏览 ========== */

function handleNodeClick(row: ShareFileNode) {
  if (row.isDir) {
    pathStack.value.push(row.id)
    selectedIds.value.clear()
  }
}

function enterDir(id: number, index: number) {
  pathStack.value = [...pathStack.value.slice(0, index + 1)]
  selectedIds.value.clear()
}

function handleSelectionChange(rows: ShareFileNode[]) {
  selectedIds.value = new Set(rows.map((r) => r.id))
}

/* ========== 预览 / 下载 ========== */

async function handlePreview(row: ShareFileNode) {
  previewing.value = row
  previewVisible.value = true
  previewLoading.value = true
  preview.value = null
  try {
    preview.value = await previewShareFile(token.value, row.id)
  } catch {
    // 错误已由拦截器提示
  } finally {
    previewLoading.value = false
  }
}

async function handleDownloadOne(row: ShareFileNode) {
  try {
    await downloadShareFile(token.value, row)
    ElMessage.success('下载已开始')
  } catch {
    // 错误已由拦截器提示
  }
}

/** 批量打包下载（含目录时后端递归打包），轮询任务状态 */
async function handleBatchDownload() {
  if (selectedIds.value.size === 0) return
  try {
    const task = await batchDownloadShare(token.value, { snapshotIds: [...selectedIds.value] })
    ElMessage.info('打包任务已创建，处理中...')
    const result = await pollTask(task.taskId)
    if (result?.status === 'DONE' && result.url) {
      downloadByUrl(result.url, `${info.value?.name ?? 'share'}-${task.taskId}.zip`)
    } else {
      ElMessage.error('打包失败，请重试')
    }
  } catch {
    // 错误已由拦截器提示
  }
}

async function pollTask(taskId: string, retries = 20): Promise<{ status: string; url: string | null } | null> {
  for (let i = 0; i < retries; i++) {
    await new Promise((resolve) => setTimeout(resolve, 2000))
    const task = await queryShareBatchTask(token.value, taskId).catch(() => null)
    if (task && (task.status === 'DONE' || task.status === 'FAILED')) return task
  }
  return null
}

/* ========== 转存 ========== */

async function handleSave() {
  if (selectedIds.value.size === 0) return
  if (!localStorage.getItem('token')) {
    ElMessage.warning('转存需要先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  saving.value = true
  try {
    await saveShareFiles(token.value, { snapshotIds: [...selectedIds.value] })
    ElMessage.success('转存成功，可在「我的文件」中查看')
    selectedIds.value.clear()
  } catch (err) {
    const e = err as { code?: number }
    if (e.code === 10306) {
      pageState.value = 'error'
      errorMessage.value = '分享已取消'
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.guest-share {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
  box-sizing: border-box;
}

.state-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}

.pwd-card {
  width: 380px;
  text-align: center;
}

.pwd-title {
  margin: 0 0 8px;
}

.pwd-sub {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
}

.pwd-input {
  margin-bottom: 12px;
}

.pwd-btn {
  width: 100%;
}

.guest-body {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.guest-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.guest-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
}

.guest-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.deny-tag {
  color: #e6a23c;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #ecf5ff;
  border-radius: 4px;
}

.batch-count {
  font-size: 13px;
  color: #409eff;
  margin-right: 8px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.crumb::after {
  content: '/';
  margin-left: 8px;
  color: #c0c4cc;
}

.crumb:last-child::after {
  content: '';
}

.node-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.node-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-body {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-media {
  max-width: 100%;
  max-height: 70vh;
  display: block;
}

.preview-audio {
  width: 100%;
}

.preview-pdf {
  width: 100%;
  height: 70vh;
  border: none;
}

.preview-text {
  width: 100%;
  max-height: 70vh;
  overflow: auto;
  margin: 0;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
