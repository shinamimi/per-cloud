<template>
  <!--
    AdminUserView —— 管理员用户管理页面。
    展示所有非管理员用户，支持搜索筛选、状态管理、配额调整、密码重置等操作。
  -->
  <div class="admin-users">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="search" inline size="default">
        <el-form-item label="用户名">
          <el-input v-model="search.username" placeholder="搜索用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width:130px">
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
            <el-option label="已锁定" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表表格 -->
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
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuota" label="总配额" width="120">
          <template #default="{ row }">{{ formatBytes(row.totalQuota) }}</template>
        </el-table-column>
        <el-table-column prop="usedSpace" label="已用空间" width="120">
          <template #default="{ row }">{{ formatBytes(row.usedSpace) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 2"
              @click="handleUnlock(row)"
            >
              解锁
            </el-button>
            <el-button link type="primary" size="small" @click="openQuotaDialog(row)">
              配额
            </el-button>
            <el-button link type="primary" size="small" @click="openPasswordDialog(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 调整配额对话框 -->
    <el-dialog v-model="quotaDialog.visible" title="调整配额" width="420px" :close-on-click-modal="false">
      <el-form :model="quotaDialog.form" label-width="100px">
        <el-form-item label="当前用户">
          <span>{{ quotaDialog.target?.username }}</span>
        </el-form-item>
        <el-form-item label="基础配额">
          <span>{{ formatBytes(quotaDialog.target?.totalQuota || 0) }}</span>
        </el-form-item>
        <el-form-item label="额外配额 (字节)">
          <el-input-number
            v-model="quotaDialog.form.adminBonusQuota"
            :min="0"
            :step="1073741824"
            style="width: 100%"
          />
          <div class="form-hint">
            当前额外配额：{{ formatBytes(quotaDialog.target?.adminBonusQuota || 0) }}，
            建议：1 GB = 1073741824, 10 GB = 10737418240
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
 * 1. 页面加载时调用 getAdminUsers() 获取所有非管理员用户
 * 2. 前端搜索过滤（用户名 + 状态），不依赖后端搜索接口（后端未提供搜索参数）
 * 3. 操作按钮：
 *   - 启用/禁用：调用 updateUserStatus(id, { status: NORMAL/DISABLED })
 *   - 解锁：调用 unlockUser(id) —— 仅对 LOCKED 状态的用户可点击
 *   - 配额：弹出对话框，设置 adminBonusQuota
 *   - 重置密码：弹出对话框，输入新密码后调用 resetUserPassword
 * 4. 每个操作完成后刷新列表，保持数据一致性
 *
 * 为什么不用后端搜索？
 * 后端 AdminUserController.listUsers 没有搜索/分页参数，返回全量用户。
 * 前端用 computed 做内存过滤，对于用户量不大的场景足够高效。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminUsers,
  updateUserStatus,
  unlockUser,
  updateUserQuota,
  resetUserPassword,
} from '@/api/admin'
import type { AdminUserResponse } from '@/types/admin'
import { AdminUserStatus, AdminUserStatusLabel, AdminUserStatusType } from '@/types/admin'

/* ========== 数据加载 ========== */

const loading = ref(false)
const users = ref<AdminUserResponse[]>([])

const search = reactive({
  username: '',
  status: null as number | null,
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

/* ========== 工具函数 ========== */

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

function statusLabel(status: number): string {
  return AdminUserStatusLabel[status as AdminUserStatus] || '未知'
}

function statusTagType(status: number): string {
  return AdminUserStatusType[status as AdminUserStatus] || 'info'
}

/* ========== 操作处理 ========== */

/** 切换用户启用/禁用状态 */
async function handleToggleStatus(row: AdminUserResponse) {
  const action = row.status === 1 ? '禁用' : '启用'
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
    const newStatus = row.status === 1 ? AdminUserStatus.DISABLED : AdminUserStatus.NORMAL
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

const quotaDialog = reactive({
  visible: false,
  loading: false,
  target: null as AdminUserResponse | null,
  form: {
    adminBonusQuota: 0,
  },
})

function openQuotaDialog(row: AdminUserResponse) {
  quotaDialog.target = row
  quotaDialog.form.adminBonusQuota = row.adminBonusQuota
  quotaDialog.visible = true
}

async function submitQuota() {
  if (!quotaDialog.target) return

  quotaDialog.loading = true
  try {
    await updateUserQuota(quotaDialog.target.id, {
      adminBonusQuota: quotaDialog.form.adminBonusQuota,
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

onMounted(loadUsers)
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

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
