<!--
  TeamsView —— 我的团队页面（/teams）。
  团队卡片列表 + 创建团队 + 成员管理 + 团队设置。
  设计依据 docs/team-module.md：OWNER 可解散；OWNER/ADMIN 可成员管理与改团队资料。
-->
<template>
  <div class="teams-view">
    <div class="teams-header">
      <span class="page-title">我的团队</span>
      <el-button type="primary" @click="createVisible = true">
        <el-icon><Plus /></el-icon>
        <span>创建团队</span>
      </el-button>
    </div>

    <el-empty v-if="teams.length === 0" description="还没有团队，创建一个开始协作吧" />
    <div v-else class="team-grid">
      <el-card v-for="t in teams" :key="t.id" shadow="never" class="team-card">
        <div class="team-head">
          <el-avatar :size="44" :src="t.avatar || undefined">
            {{ t.name[0] }}
          </el-avatar>
          <div class="team-info">
            <div class="team-name">{{ t.name }}</div>
            <div class="team-sub">
              {{ t.memberCount }} 名成员 · 创建者 {{ t.ownerName || '未知' }}
            </div>
          </div>
          <el-tag v-if="t.myRole === 'OWNER'" type="danger" size="small">创建者</el-tag>
          <el-tag v-else-if="t.myRole === 'ADMIN'" type="warning" size="small">管理员</el-tag>
        </div>

        <div class="quota-bar">
          <el-progress
            :percentage="quotaPercent(t)"
            :status="quotaPercent(t) >= 100 ? 'exception' : undefined"
          />
          <div class="quota-text">
            {{ formatBytesAuto(t.usedSpace) }} / {{ formatBytesAuto(t.quota) }}
          </div>
        </div>

        <div class="team-actions">
          <el-button size="small" type="primary" @click="enterTeam(t)">进入团队</el-button>
          <el-button
            size="small"
            :disabled="t.myRole !== 'OWNER' && t.myRole !== 'ADMIN'"
            @click="openMembers(t)"
          >
            成员管理
          </el-button>
          <el-button
            size="small"
            :disabled="t.myRole !== 'OWNER' && t.myRole !== 'ADMIN'"
            @click="openSettings(t)"
          >
            团队设置
          </el-button>
          <el-dropdown>
            <el-button size="small" :disabled="t.myRole === 'OWNER'">退出</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLeave(t)">退出团队</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="t.myRole === 'OWNER'" size="small" type="danger" plain @click="handleDissolve(t)">
            解散
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 创建团队 -->
    <el-dialog v-model="createVisible" title="创建团队" width="440px" draggable>
      <el-form label-width="70px">
        <el-form-item label="团队名称" required>
          <el-input v-model="createForm.name" placeholder="1-64 个字符" maxlength="64" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="2"
            placeholder="团队简介（可选）"
            maxlength="255"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理 -->
    <el-dialog v-model="membersVisible" :title="`成员管理 - ${currentTeam?.name ?? ''}`" width="560px" draggable>
      <div class="invite-bar">
        <el-input v-model="inviteKeyword" placeholder="按用户名/昵称搜索用户" clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="handleSearchInvite">搜索</el-button>
      </div>
      <div v-if="inviteResults.length > 0" class="invite-results">
        <div v-for="u in inviteResults" :key="u.userId" class="invite-row">
          <el-avatar :size="30" :src="u.avatar || undefined">
            {{ (u.nickname || u.username)[0] }}
          </el-avatar>
          <span class="invite-name">{{ u.nickname || u.username }}（{{ u.username }}）</span>
          <el-button
            size="small"
            type="primary"
            :disabled="isMember(u.userId)"
            @click="handleInvite(u)"
          >
            {{ isMember(u.userId) ? '已在团队' : '邀请' }}
          </el-button>
        </div>
      </div>
      <el-divider />
      <el-table :data="members" size="small">
        <el-table-column label="成员" min-width="160">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="28" :src="row.avatar || undefined">
                {{ (row.nickname || row.username)[0] }}
              </el-avatar>
              <span>{{ row.nickname || row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button
              v-if="canRemove(row)"
              size="small"
              type="danger"
              plain
              @click="handleRemoveMember(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 团队设置 -->
    <el-dialog v-model="settingsVisible" :title="`团队设置 - ${currentTeam?.name ?? ''}`" width="440px" draggable>
      <el-form label-width="70px">
        <el-form-item label="团队名称">
          <el-input v-model="settingsForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="settingsForm.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateTeam">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createTeam,
  dissolveTeam,
  getMyTeams,
  getTeamMembers,
  inviteMembers,
  leaveTeam,
  removeTeamMember,
  updateTeam,
} from '@/api/team'
import { searchUsers } from '@/api/friend'
import { formatBytesAuto } from '@/utils/format'
import type { Team, TeamMember, TeamRole } from '@/types/team'
import type { FriendUser } from '@/types/friend'

const router = useRouter()
const teams = ref<Team[]>([])

/* ========== 创建 ========== */
const createVisible = ref(false)
const createForm = ref({ name: '', description: '' })

async function handleCreate() {
  const name = createForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入团队名称')
    return
  }
  await createTeam({ name, description: createForm.value.description.trim() || undefined })
  ElMessage.success('团队创建成功')
  createVisible.value = false
  createForm.value = { name: '', description: '' }
  await loadTeams()
}

/* ========== 成员管理 ========== */
const membersVisible = ref(false)
const currentTeam = ref<Team | null>(null)
const members = ref<TeamMember[]>([])
const inviteKeyword = ref('')
const inviteResults = ref<FriendUser[]>([])

function openMembers(t: Team) {
  currentTeam.value = t
  membersVisible.value = true
  inviteResults.value = []
  void loadMembers()
}

async function loadMembers() {
  if (!currentTeam.value) return
  members.value = await getTeamMembers(currentTeam.value.id)
}

function isMember(userId: number): boolean {
  return members.value.some((m) => m.userId === userId)
}

async function handleSearchInvite() {
  const k = inviteKeyword.value.trim()
  if (!k) return
  inviteResults.value = (await searchUsers(k)).filter((u) => u.relation !== 'FRIEND' || true)
}

async function handleInvite(u: FriendUser) {
  if (!currentTeam.value) return
  await inviteMembers(currentTeam.value.id, { userIds: [u.userId] })
  ElMessage.success(`已邀请 ${u.nickname || u.username}`)
  inviteResults.value = []
  await loadMembers()
}

function canRemove(row: TeamMember): boolean {
  const me = currentTeam.value?.myRole
  if (row.role === 'OWNER') return false
  if (me === 'OWNER') return true
  if (me === 'ADMIN') return row.role !== 'ADMIN'
  return false
}

async function handleRemoveMember(row: TeamMember) {
  if (!currentTeam.value) return
  try {
    await ElMessageBox.confirm(`确定移除成员「${row.nickname || row.username}」吗？`, '移除成员', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await removeTeamMember(currentTeam.value.id, row.userId)
  ElMessage.success('已移除')
  await loadMembers()
  await loadTeams()
}

/* ========== 团队设置 ========== */
const settingsVisible = ref(false)
const settingsForm = ref({ name: '', description: '' })

function openSettings(t: Team) {
  currentTeam.value = t
  settingsForm.value = { name: t.name, description: t.description ?? '' }
  settingsVisible.value = true
}

async function handleUpdateTeam() {
  if (!currentTeam.value) return
  await updateTeam(currentTeam.value.id, {
    name: settingsForm.value.name.trim() || undefined,
    description: settingsForm.value.description.trim() || undefined,
  })
  ElMessage.success('已保存')
  settingsVisible.value = false
  await loadTeams()
}

/* ========== 退出 / 解散 ========== */

async function handleLeave(t: Team) {
  try {
    await ElMessageBox.confirm(`确定退出团队「${t.name}」吗？`, '退出团队', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await leaveTeam(t.id)
  ElMessage.success('已退出团队')
  await loadTeams()
}

async function handleDissolve(t: Team) {
  try {
    await ElMessageBox.confirm(
      `确定解散团队「${t.name}」吗？团队文件将进入团队回收站，此操作不可恢复。`,
      '解散团队',
      {
        confirmButtonText: '解散',
        cancelButtonText: '取消',
        type: 'error',
      },
    )
  } catch {
    return
  }
  await dissolveTeam(t.id)
  ElMessage.success('团队已解散')
  await loadTeams()
}

/* ========== 工具 ========== */

function enterTeam(t: Team) {
  router.push(`/teams/${t.id}/files`)
}

function quotaPercent(t: Team): number {
  if (!t.quota) return 0
  return Math.min(100, Math.round((t.usedSpace / t.quota) * 100))
}

function roleLabel(role: TeamRole): string {
  return { OWNER: '创建者', ADMIN: '管理员', MEMBER: '成员' }[role]
}

function roleTagType(role: TeamRole): 'danger' | 'warning' | 'info' {
  return { OWNER: 'danger', ADMIN: 'warning', MEMBER: 'info' }[role] as 'danger' | 'warning' | 'info'
}

async function loadTeams() {
  teams.value = await getMyTeams()
}

onMounted(() => {
  void loadTeams()
})
</script>

<style scoped>
.teams-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.teams-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 12px;
}

.team-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.team-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-size: 15px;
  font-weight: 600;
}

.team-sub {
  font-size: 12px;
  color: #909399;
}

.quota-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-bar .el-progress {
  flex: 1;
}

.quota-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.team-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.invite-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.invite-results {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 180px;
  overflow: auto;
}

.invite-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.invite-name {
  flex: 1;
  font-size: 13px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
