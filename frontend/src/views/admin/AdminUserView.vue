<template>
  <!--
    AdminUserView —— 管理员用户管理页面。
    展示所有非管理员用户，支持搜索筛选、状态管理、配额调整、密码重置等操作。
  -->
  <div class="admin-users">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索栏：状态下拉从字典 userStatus 组渲染（frontend-standard.md） -->
    <el-card shadow="never" class="search-card">
      <el-form :model="search" inline size="default">
        <el-form-item label="用户名">
          <el-input v-model="search.username" placeholder="搜索用户名" clearable @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="opt in metaStore.getGroup(MetaGroup.USER_STATUS)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表表格：容量列自动选择单位（B/KB/MB/GB/TB）只读展示 -->
    <el-card shadow="never">
      <el-table
        :data="filteredUsers"
        v-loading="loading"
        stripe
        style="width: 100%"
        size="default"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="权限等级" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuota" label="总配额" width="120">
          <template #default="{ row }">{{ formatBytesAuto(row.totalQuota) }}</template>
        </el-table-column>
        <el-table-column prop="usedSpace" label="已用空间" width="120">
          <template #default="{ row }">{{ formatBytesAuto(row.usedSpace) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
        </el-table-column>

        <!-- 操作列：按钮显隐全部由 can() 规则表推导（frontend-standard.md 5.x） -->
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('disable', userStore.role, row.status, USER_OPERATIONS)"
              link
              type="danger"
              size="small"
              @click="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-if="can('enable', userStore.role, row.status, USER_OPERATIONS)"
              link
              type="primary"
              size="small"
              @click="handleToggleStatus(row)"
            >
              启用
            </el-button>
            <el-button
              v-if="can('unlock', userStore.role, row.status, USER_OPERATIONS)"
              link
              type="primary"
              size="small"
              @click="handleUnlock(row)"
            >
              解锁
            </el-button>
            <el-button
              v-if="can('quota', userStore.role, row.status, USER_OPERATIONS)"
              link
              type="primary"
              size="small"
              @click="openQuotaDialog(row)"
            >
              配额
            </el-button>
            <el-button
              v-if="can('resetPwd', userStore.role, row.status, USER_OPERATIONS)"
              link
              type="primary"
              size="small"
              @click="openPasswordDialog(row)"
            >
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!--
      调整配额对话框。
      单位选择在弹窗内进行：
      - 基础配额：只读展示，固定以 GB 为单位
      - 额外配额：按所选单位（KB/MB/GB，默认 MB）输入，提交时换算为字节
    -->
    <el-dialog v-model="quotaDialog.visible" title="调整配额" width="420px" :close-on-click-modal="false">
      <el-form :model="quotaDialog" label-width="100px">
        <el-form-item label="当前用户">
          <span>{{ quotaDialog.target?.username }}</span>
        </el-form-item>
        <el-form-item label="基础配额">
          <span>{{ formatSize(quotaDialog.target?.totalQuota || 0, 'GB') }} GB</span>
        </el-form-item>
        <el-form-item label="额外配额">
          <div class="quota-input-row">
            <el-input-number
              v-model="quotaDialog.inputValue"
              :min="0"
              :step="1"
              style="flex: 1"
            />
            <el-select v-model="quotaDialog.unit" size="default" style="width: 90px">
              <el-option v-for="unit in SIZE_UNITS" :key="unit" :label="unit" :value="unit" />
            </el-select>
          </div>
          <div class="form-hint">
            当前额外配额：{{ formatSize(quotaDialog.target?.adminBonusQuota || 0, quotaDialog.unit) }}
            {{ quotaDialog.unit }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="quotaDialog.loading" @click="submitQuota">确认</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form :model="pwdDialog.form" label-width="100px">
        <el-form-item label="目标用户">
          <span>{{ pwdDialog.target?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdDialog.form.newPassword"
            type="password"
            show-password
            placeholder="9位以上，包含字母和数字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialog.loading" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/*
 * 管理员用户管理页面 —— 核心逻辑：
 *
 * 1. 字典驱动：状态筛选下拉与状态标签 label 来自 metaStore 字典组
 *    （GET /api/meta/options 的 userStatus 组），前端只维护 Tag 颜色。
 * 2. 权限驱动：操作按钮显隐由 can() 规则表推导，
 *    页面不写死 role/status 判断（见 src/permissions/admin-operations.ts）。
 * 3. 前端内存过滤：后端 listUsers 无搜索参数，用户量不大时 computed 过滤足够。
 *
 * 注意：后端枚举序列化为字符串（"NORMAL" 等），
 * 所有状态比较一律使用字符串，与字典 value 对齐。
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminUsers,
  updateUserStatus,
  unlockUser,
  updateUserQuota,
  resetUserPassword,
} from '@/api/admin/user'
import type { AdminUserResponse, RoleKey, UserStatusKey } from '@/types/admin'
import { ROLE_TAG_TYPE, USER_STATUS_TAG_TYPE } from '@/types/admin'
import { MetaGroup } from '@/types/meta'
import { useMetaStore } from '@/stores/meta'
import { useUserStore } from '@/stores/user'
import { can } from '@/utils/permission'
import { USER_OPERATIONS } from '@/permissions/admin-operations'
import {
  formatSize,
  formatBytesAuto,
  SIZE_UNITS,
  UNIT_BYTES,
  type SizeUnit,
} from '@/utils/format'

const metaStore = useMetaStore()
const userStore = useUserStore()

/* ========== 数据加载 ========== */

const loading = ref(false)
const users = ref<AdminUserResponse[]>([])

const search = reactive({
  username: '',
  status: null as string | null,
})

/** 前端过滤后的用户列表 */
const filteredUsers = computed(() => {
  return users.value.filter((u) => {
    if (search.username && !u.username.includes(search.username)) return false
    if (search.status !== null && u.status !== search.status) return false
    return true
  })
})

async function loadUsers() {
  loading.value = true
  try {
    users.value = await getAdminUsers()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.username = ''
  search.status = null
}

/* ========== 字典驱动的展示 ========== */

/** 状态 label 从字典 userStatus 组获取；字典未加载时回退为原始字符串 */
function statusLabel(status: UserStatusKey): string {
  return (
    metaStore.getGroup(MetaGroup.USER_STATUS).find((opt) => opt.value === status)?.label ??
    status
  )
}

/** Tag 颜色属于前端 UI 样式，前端维护映射（字典不返回颜色） */
function statusTagType(status: UserStatusKey): string {
  return USER_STATUS_TAG_TYPE[status] ?? 'info'
}

/** 角色 label 从字典 role 组获取；字典未加载时回退为原始字符串 */
function roleLabel(role: RoleKey): string {
  return (
    metaStore.getGroup(MetaGroup.ROLE).find((opt) => opt.value === role)?.label ?? role
  )
}

/** 角色 Tag 颜色 —— 共用映射（types/admin.ts ROLE_TAG_TYPE，与管理页保持一致） */
function roleTagType(role: RoleKey): string {
  return ROLE_TAG_TYPE[role] ?? 'info'
}

/* ========== 工具函数 ========== */

/* 容量展示说明：表格列使用 formatBytesAuto 自动单位；
 * 配额弹窗内由用户选择单位（见下方 quotaDialog.unit，默认 MB）。 */

/* ========== 操作处理 ========== */

/** 切换用户启用/禁用状态 */
async function handleToggleStatus(row: AdminUserResponse) {
  const isDisabled = row.status === 'DISABLED'
  const action = isDisabled ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    const newStatus: UserStatusKey = isDisabled ? 'NORMAL' : 'DISABLED'
    await updateUserStatus(row.id, { status: newStatus })
    ElMessage.success(`${action}成功`)
    await loadUsers()
  } catch {
    // 错误已在拦截器中提示
  }
}

/** 解锁用户 */
async function handleUnlock(row: AdminUserResponse) {
  try {
    await ElMessageBox.confirm(`确定要解锁用户「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await unlockUser(row.id)
    ElMessage.success('解锁成功')
    await loadUsers()
  } catch {
    // 错误已在拦截器中提示
  }
}

/* ========== 配额对话框 ========== */

/**
 * 单位选择在弹窗内进行：
 * - unit      额外配额输入单位，默认 MB（基础配额固定以 GB 展示）
 * - inputValue 按当前单位输入的额外配额数值，提交时换算为字节
 */
const quotaDialog = reactive({
  visible: false,
  loading: false,
  target: null as AdminUserResponse | null,
  unit: 'MB' as SizeUnit,
  inputValue: 0,
})

/** 切换单位时换算输入值，保持物理量（字节数）不变 */
watch(
  () => quotaDialog.unit,
  (newUnit, oldUnit) => {
    quotaDialog.inputValue =
      (quotaDialog.inputValue * UNIT_BYTES[oldUnit]) / UNIT_BYTES[newUnit]
  },
)

function openQuotaDialog(row: AdminUserResponse) {
  quotaDialog.target = row
  quotaDialog.unit = 'MB'
  quotaDialog.inputValue = row.adminBonusQuota / UNIT_BYTES.MB
  quotaDialog.visible = true
}

async function submitQuota() {
  if (!quotaDialog.target) return

  quotaDialog.loading = true
  try {
    await updateUserQuota(quotaDialog.target.id, {
      adminBonusQuota: Math.round(quotaDialog.inputValue * UNIT_BYTES[quotaDialog.unit]),
    })
    ElMessage.success('配额调整成功')
    quotaDialog.visible = false
    await loadUsers()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    quotaDialog.loading = false
  }
}

/* ========== 重置密码对话框 ========== */

const pwdDialog = reactive({
  visible: false,
  loading: false,
  target: null as AdminUserResponse | null,
  form: {
    newPassword: '',
  },
})

function openPasswordDialog(row: AdminUserResponse) {
  pwdDialog.target = row
  pwdDialog.form.newPassword = ''
  pwdDialog.visible = true
}

async function submitPassword() {
  if (!pwdDialog.target) return

  const pwd = pwdDialog.form.newPassword
  if (pwd.length < 9) {
    ElMessage.warning('密码长度必须大于 8 位')
    return
  }
  if (!/(?=.*[a-zA-Z])(?=.*\d)/.test(pwd)) {
    ElMessage.warning('密码必须同时包含字母和数字')
    return
  }

  pwdDialog.loading = true
  try {
    await resetUserPassword(pwdDialog.target.id, { newPassword: pwd })
    ElMessage.success('密码已重置')
    pwdDialog.visible = false
  } catch {
    // 错误已在拦截器中提示
  } finally {
    pwdDialog.loading = false
  }
}

onMounted(() => {
  // 字典拉取失败不影响页面：loadIfNeeded 内部已静默降级
  metaStore.loadIfNeeded()
  loadUsers()
})
</script>

<style scoped>
.admin-users {
  max-width: 1200px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.search-card {
  margin-bottom: 16px;
}

/* 额外配额输入行：数值输入框 + 单位选择器 */
.quota-input-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
