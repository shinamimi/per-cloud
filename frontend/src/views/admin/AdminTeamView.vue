<!--
  AdminTeamView —— 管理后台团队管理页面（/admin/teams）。
  团队列表（真实成员数/配额/用量）+ 配额调整 + 详情（成员）+ 文件/回收站只读 + 强制解散。
  权限：后端 /api/admin/** 已由 SecurityConfig 拦截（OPERATOR 及以上）。
-->
<template>
  <div class="admin-team-view">
    <el-card shadow="never">
      <div class="card-header">
        <span class="card-title">团队管理（{{ teams.length }}）</span>
      </div>

      <el-table v-loading="loading" :data="teams">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="团队" min-width="180">
          <template #default="{ row }">
            <div class="team-cell">
              <el-avatar :size="30" :src="row.avatar || undefined">
                {{ row.name[0] }}
              </el-avatar>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ownerId" label="创建者 ID" width="100" />
        <el-table-column prop="memberCount" label="成员" width="80" />
        <el-table-column label="配额用量" min-width="170">
          <template #default="{ row }">
            <div class="quota-cell">
              <el-progress
                :percentage="quotaPercent(row)"
                :status="quotaPercent(row) >= 100 ? 'exception' : undefined"
              />
              <span class="quota-text">
                {{ formatBytesAuto(row.usedSpace) }} / {{ formatBytesAuto(row.quota) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openQuota(row)">调整配额</el-button>
            <el-button link type="primary" @click="openFiles(row)">文件</el-button>
            <el-button link type="primary" @click="openRecycle(row)">回收站</el-button>
            <el-button link type="danger" @click="handleDissolve(row)">解散</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 调整配额 -->
    <el-dialog v-model="quotaDialog.visible" title="调整团队配额" width="440px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="团队">
          <span>{{ quotaDialog.target?.name }}</span>
        </el-form-item>
        <el-form-item label="当前用量">
          <span>{{ formatBytesAuto(quotaDialog.target?.usedSpace || 0) }}</span>
        </el-form-item>
        <el-form-item label="配额">
          <div class="quota-input-row">
            <el-input-number
              v-model="quotaDialog.inputValue"
              :min="0"
              :step="1"
              style="flex: 1"
            />
            <el-select v-model="quotaDialog.unit" style="width: 90px">
              <el-option v-for="unit in SIZE_UNITS" :key="unit" :label="unit" :value="unit" />
            </el-select>
          </div>
          <div class="form-hint">新配额不能小于团队已用空间</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="quotaDialog.loading" @click="submitQuota">确认</el-button>
      </template>
    </el-dialog>

    <!-- 详情：基本信息 + 成员 -->
    <el-dialog v-model="detailVisible" :title="`团队详情 - ${detailTeam?.name ?? ''}`" width="560px" draggable>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="创建者 ID">{{ detailTeam?.ownerId }}</el-descriptions-item>
        <el-descriptions-item label="成员数">{{ detailTeam?.memberCount }}</el-descriptions-item>
        <el-descriptions-item label="配额">{{ formatBytesAuto(detailTeam?.quota || 0) }}</el-descriptions-item>
        <el-descriptions-item label="已用">{{ formatBytesAuto(detailTeam?.usedSpace || 0) }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detailTeam?.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailMembers" size="small" class="member-table">
        <el-table-column label="成员" min-width="150">
          <template #default="{ row }">
            <span>{{ row.nickname || row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 团队文件（只读） -->
    <el-dialog v-model="filesVisible" :title="`团队文件 - ${filesTeam?.name ?? ''}`" width="760px" draggable>
      <el-breadcrumb separator="/" class="file-breadcrumb">
        <el-breadcrumb-item @click="filesNav(0)">全部文件</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(d, i) in filesStack" :key="d.id" @click="filesNav(d.id, i + 1)">
          {{ d.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      <el-table v-loading="filesLoading" :data="files" size="small" @row-dblclick="filesOpenDir">
        <el-table-column label="名称" min-width="220">
          <template #default="{ row }">
            <div class="file-name">
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
        <el-table-column label="创建人" width="110">
          <template #default="{ row }">
            {{ row.uploaderName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
      </el-table>
    </el-dialog>
    <!-- 团队回收站（管理端可查看 + 提前物理清除） -->
    <el-dialog v-model="recycleVisible" :title="`团队回收站 - ${recycleTeam?.name ?? ''}`" width="760px" draggable>
      <el-table v-loading="recycleLoading" :data="recycleItems" size="small">
        <el-table-column label="名称" min-width="220">
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
        <el-table-column prop="deletedTime" label="删除时间" width="160" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="danger" @click="handlePurgeRecycle(row)">清除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!recycleLoading && recycleItems.length === 0" description="回收站是空的" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Folder } from '@element-plus/icons-vue'
import {
  dissolveAdminTeam,
  getAdminTeamDetail,
  getAdminTeamFiles,
  getAdminTeams,
  purgeAdminTeamRecycle,
  updateAdminTeamQuota,
} from '@/api/admin/team'
import { getAdminTeamRecycleBin } from '@/api/admin/team'
import { formatBytesAuto, SIZE_UNITS, UNIT_BYTES, type SizeUnit } from '@/utils/format'
import type { FileItem, RecycleBinItem } from '@/types/file'
import type { Team, TeamMember, TeamRole } from '@/types/team'

const loading = ref(false)
const teams = ref<Team[]>([])

async function loadTeams() {
  loading.value = true
  try {
    teams.value = await getAdminTeams()
  } finally {
    loading.value = false
  }
}

function quotaPercent(t: Team): number {
  if (!t.quota) return 0
  return Math.min(100, Math.round((t.usedSpace / t.quota) * 100))
}

/* ========== 配额调整 ========== */

const quotaDialog = reactive<{
  visible: boolean
  loading: boolean
  target: Team | null
  inputValue: number
  unit: SizeUnit
}>({ visible: false, loading: false, target: null, inputValue: 0, unit: 'GB' })

function openQuota(t: Team) {
  quotaDialog.target = t
  quotaDialog.unit = 'GB'
  quotaDialog.inputValue = Math.round(t.quota / UNIT_BYTES.GB)
  quotaDialog.visible = true
}

async function submitQuota() {
  if (!quotaDialog.target) return
  const bytes = Math.round(quotaDialog.inputValue * UNIT_BYTES[quotaDialog.unit])
  if (bytes <= 0) {
    ElMessage.warning('配额必须大于 0')
    return
  }
  quotaDialog.loading = true
  try {
    await updateAdminTeamQuota(quotaDialog.target.id, { adminBonusQuota: bytes })
    ElMessage.success('配额已更新')
    quotaDialog.visible = false
    await loadTeams()
  } finally {
    quotaDialog.loading = false
  }
}

/* ========== 详情 ========== */

const detailVisible = ref(false)
const detailTeam = ref<Team | null>(null)
const detailMembers = ref<TeamMember[]>([])

async function openDetail(t: Team) {
  const res = await getAdminTeamDetail(t.id)
  detailTeam.value = res.team
  detailMembers.value = res.members
  detailVisible.value = true
}

/* ========== 团队文件 ========== */

const filesVisible = ref(false)
const filesTeam = ref<Team | null>(null)
const files = ref<FileItem[]>([])
const filesLoading = ref(false)
const filesStack = ref<Array<{ id: number; name: string }>>([])

async function openFiles(t: Team) {
  filesTeam.value = t
  filesStack.value = []
  filesVisible.value = true
  await filesLoad()
}

async function filesLoad() {
  if (!filesTeam.value) return
  filesLoading.value = true
  try {
    const parentId = filesStack.value[filesStack.value.length - 1]?.id ?? 0
    const res = await getAdminTeamFiles(filesTeam.value.id, parentId, 1, 50)
    files.value = res.records
  } finally {
    filesLoading.value = false
  }
}

function filesNav(id: number, depth?: number) {
  if (depth !== undefined) {
    filesStack.value = filesStack.value.slice(0, depth)
  } else {
    filesStack.value = []
  }
  void filesLoad()
}

function filesOpenDir(row: FileItem) {
  if (!row.isDirectory) return
  filesStack.value.push({ id: row.id, name: row.name })
  void filesLoad()
}

/* ========== 团队回收站 ========== */

const recycleVisible = ref(false)
const recycleTeam = ref<Team | null>(null)
const recycleItems = ref<RecycleBinItem[]>([])
const recycleLoading = ref(false)

async function openRecycle(t: Team) {
  recycleTeam.value = t
  recycleVisible.value = true
  await recycleLoad()
}

async function recycleLoad() {
  if (!recycleTeam.value) return
  recycleLoading.value = true
  try {
    recycleItems.value = await getAdminTeamRecycleBin(recycleTeam.value.id)
  } finally {
    recycleLoading.value = false
  }
}

async function handlePurgeRecycle(row: RecycleBinItem) {
  if (!recycleTeam.value) return
  try {
    await ElMessageBox.confirm(
      `确定清除「${row.originalName}」吗？文件数据将不可恢复。`,
      '物理清除',
      { confirmButtonText: '清除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await purgeAdminTeamRecycle(recycleTeam.value.id, row.id)
  ElMessage.success('已清除')
  await recycleLoad()
}

/* ========== 解散 ========== */

async function handleDissolve(t: Team) {
  try {
    await ElMessageBox.confirm(
      `确定强制解散团队「${t.name}」吗？所有成员将失去访问权限，团队文件进入团队回收站。`,
      '解散团队',
      { confirmButtonText: '解散', cancelButtonText: '取消', type: 'error' },
    )
  } catch {
    return
  }
  await dissolveAdminTeam(t.id)
  ElMessage.success('团队已解散')
  await loadTeams()
}

function roleLabel(role: TeamRole): string {
  return { OWNER: '创建者', ADMIN: '管理员', MEMBER: '成员' }[role]
}

function roleTagType(role: TeamRole): 'danger' | 'warning' | 'info' {
  return { OWNER: 'danger', ADMIN: 'warning', MEMBER: 'info' }[role] as 'danger' | 'warning' | 'info'
}

onMounted(() => {
  void loadTeams()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-cell .el-progress {
  flex: 1;
  max-width: 120px;
}

.quota-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.quota-input-row {
  display: flex;
  gap: 8px;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.member-table {
  margin-top: 12px;
}

.file-breadcrumb {
  margin-bottom: 12px;
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
