<template>
  <!--
    AdminAdminView —— 管理员账户管理页面。
    仅 SUPER_ADMIN 可访问（后端 SecurityConfig 限制 /api/admin/admins/**）。
    提供两种管理方式：
    - 「添加管理员」入口：弹出穿梭器，批量调整候选人角色
    - 「创建管理员」对话框：单个创建管理员账户（与穿梭器并存）
  -->
  <div class="admin-admins">
    <h2 class="page-title">管理员管理</h2>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="openTransferDialog">添加管理员</el-button>
      <el-button @click="openCreateDialog">创建管理员</el-button>
    </div>

    <!-- 管理员列表 -->
    <el-card shadow="never">
      <el-table :data="admins" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditRoleDialog(row)">
              修改角色
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 穿梭器对话框（添加管理员） -->
    <el-dialog v-model="transferDialog.visible" title="添加管理员" width="680px" :close-on-click-modal="false">
      <!--
        加载中：候选列表/管理员列表尚未返回时不渲染 Transfer，
        避免左右列表误显示 0 误导用户；用骨架屏提示数据加载中。
      -->
      <div v-if="transferDialog.loading" class="transfer-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <Transfer
        v-else
        :candidates="transferDialog.candidates"
        :selected="transferDialog.selected"
        :role-options="transferRoleOptions"
        :current-user-id="userStore.userId"
        @confirm="handleTransferConfirm"
        @cancel="transferDialog.visible = false"
      />
    </el-dialog>

    <!-- 创建管理员对话框 -->
    <el-dialog v-model="createDialog.visible" title="创建管理员" width="480px" :close-on-click-modal="false">
      <el-form :model="createDialog.form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="createDialog.form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="createDialog.form.password"
            type="password"
            show-password
            placeholder="9位以上，包含字母和数字"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createDialog.form.email" placeholder="管理员邮箱" />
        </el-form-item>
        <el-form-item label="昵称（可选）">
          <el-input v-model="createDialog.form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createDialog.form.role" style="width: 100%">
            <el-option
              v-for="opt in transferRoleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="createDialog.loading" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 修改角色对话框 -->
    <el-dialog v-model="editRoleDialog.visible" title="修改角色" width="420px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="当前用户">
          <span>{{ editRoleDialog.target?.username }}</span>
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag :type="roleTagType(editRoleDialog.target?.role ?? 'ADMIN')" size="small">
            {{ roleLabel(editRoleDialog.target?.role || 'ADMIN') }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="editRoleDialog.newRole" style="width: 100%">
            <el-option
              v-for="opt in transferRoleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRoleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editRoleDialog.loading" @click="submitEditRole">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/*
 * 管理员账户管理页面。
 *
 * 设计思路：
 * 1. 「添加管理员」穿梭器：
 *    - 打开时父组件拉取候选列表（GET /api/admin/admins/candidates）传入 Transfer
 *    - 角色档位来自字典 role 组过滤（排除 USER 和 SUPER_ADMIN，剩 OPERATOR/ADMIN 两档）
 *    - 确认后调用批量接口 PUT /api/admin/admins/batch（降级也传 USER 目标角色）
 * 2. 「创建管理员」对话框：单个创建，与穿梭器并存，适用于需要指定账号密码的场景
 * 3. 角色 label 从字典获取（后端已混淆：OPERATOR→管理员，ADMIN→超级管理员），
 *    前端零二次映射；Tag 颜色属前端 UI 样式，本地维护。
 *
 * 权限说明：本页所有接口仅 SUPER_ADMIN 可调用，
 * 权限不足时后端返回 403，前端不重复校验（路由层已有 requiresAdmin 粗筛）。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminAccounts,
  createAdminAccount,
  deleteAdminAccount,
  updateAdminRole,
  getAdminCandidates,
  updateAdminRolesBatch,
} from '@/api/admin/admin'
import type { AdminUserResponse, AdminCandidate, AdminRoleChange, RoleKey } from '@/types/admin'
import { ROLE_TAG_TYPE } from '@/types/admin'
import { MetaGroup } from '@/types/meta'
import { useMetaStore } from '@/stores/meta'
import { useUserStore } from '@/stores/user'
import Transfer from '@/components/common/Transfer.vue'

const metaStore = useMetaStore()
const userStore = useUserStore()

/* ========== 数据加载 ========== */

const loading = ref(false)
const admins = ref<AdminUserResponse[]>([])

async function loadAdmins() {
  loading.value = true
  try {
    admins.value = await getAdminAccounts()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}

/** 角色 label 从字典 role 组获取（后端已做显示层混淆），字典未加载时回退为原始值 */
function roleLabel(role: RoleKey): string {
  return metaStore.getGroup(MetaGroup.ROLE).find((opt) => opt.value === role)?.label ?? role
}

/** 角色 Tag 颜色 —— 共用映射（types/admin.ts ROLE_TAG_TYPE，与用户管理页保持一致） */
function roleTagType(role: RoleKey): string {
  return ROLE_TAG_TYPE[role] ?? 'info'
}

/* ========== 穿梭器（添加管理员） ========== */

/**
 * 可选角色档位 —— 来自字典 role 组过滤。
 * 排除 USER（普通用户档）和 SUPER_ADMIN（不暴露在此页 UI），
 * 剩余 OPERATOR（显示"管理员"）与 ADMIN（显示"超级管理员"）两档。
 * 非超级管理员（ADMIN 档）不可操作 ADMIN 权限，仅剩 OPERATOR 一档。
 */
const transferRoleOptions = computed(() =>
  metaStore
    .getGroup(MetaGroup.ROLE)
    .filter((opt) => opt.value !== 'USER' && opt.value !== 'SUPER_ADMIN')
    .filter((opt) => userStore.isSuperAdmin || opt.value !== 'ADMIN'),
)

const transferDialog = reactive({
  visible: false,
  /** 数据加载中 —— 为 true 时不渲染 Transfer，显示骨架屏 */
  loading: false,
  candidates: [] as AdminCandidate[],
  selected: [] as { id: number; username: string; nickname: string; role: RoleKey }[],
})

/**
 * 打开穿梭器：并行拉取候选列表与当前管理员列表，传入 Transfer。
 * 数据未就绪时 loading=true，dialog 内显示骨架屏而非空列表。
 */
async function openTransferDialog() {
  transferDialog.visible = true
  transferDialog.loading = true
  try {
    const [candidates, adminList] = await Promise.all([getAdminCandidates(), getAdminAccounts()])
    transferDialog.candidates = candidates
    transferDialog.selected = adminList.map((a) => ({
      id: a.id,
      username: a.username,
      nickname: a.nickname,
      role: a.role,
    }))
  } catch {
    // 拉取失败直接关闭对话框（错误信息已在拦截器中提示）
    transferDialog.visible = false
  } finally {
    transferDialog.loading = false
  }
}

/** 穿梭器确认：调用批量角色变更接口后刷新列表 */
async function handleTransferConfirm(changes: AdminRoleChange[]) {
  if (changes.length === 0) {
    transferDialog.visible = false
    return
  }
  try {
    await updateAdminRolesBatch(changes)
    ElMessage.success(`已批量更新 ${changes.length} 个用户的角色`)
    transferDialog.visible = false
    await loadAdmins()
  } catch {
    // 错误已在拦截器中提示
  }
}

/* ========== 创建管理员 ========== */

const createDialog = reactive({
  visible: false,
  loading: false,
  form: {
    username: '',
    password: '',
    email: '',
    nickname: '',
    role: 'OPERATOR' as RoleKey,
  },
})

function openCreateDialog() {
  createDialog.form = {
    username: '',
    password: '',
    email: '',
    nickname: '',
    role: 'OPERATOR',
  }
  createDialog.visible = true
}

async function submitCreate() {
  const { username, password, email, nickname, role } = createDialog.form
  if (!username || !password || !email) {
    ElMessage.warning('请填写必填字段')
    return
  }

  createDialog.loading = true
  try {
    await createAdminAccount({ username, password, email, nickname: nickname || undefined, role })
    ElMessage.success('管理员创建成功')
    createDialog.visible = false
    await loadAdmins()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    createDialog.loading = false
  }
}

/* ========== 修改角色 ========== */

const editRoleDialog = reactive({
  visible: false,
  loading: false,
  target: null as AdminUserResponse | null,
  newRole: 'OPERATOR' as RoleKey,
})

function openEditRoleDialog(row: AdminUserResponse) {
  editRoleDialog.target = row
  editRoleDialog.newRole = row.role === 'SUPER_ADMIN' ? 'ADMIN' : row.role
  editRoleDialog.visible = true
}

async function submitEditRole() {
  if (!editRoleDialog.target) return

  editRoleDialog.loading = true
  try {
    await updateAdminRole(editRoleDialog.target.id, { role: editRoleDialog.newRole })
    ElMessage.success('角色修改成功')
    editRoleDialog.visible = false
    await loadAdmins()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    editRoleDialog.loading = false
  }
}

/* ========== 删除管理员 ========== */

async function handleDelete(row: AdminUserResponse) {
  try {
    await ElMessageBox.confirm(
      `确定要删除管理员「${row.username}」吗？此操作不可恢复。`,
      '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' },
    )
  } catch {
    return
  }

  try {
    await deleteAdminAccount(row.id)
    ElMessage.success('管理员已删除')
    await loadAdmins()
  } catch {
    // 错误已在拦截器中提示
  }
}

onMounted(() => {
  metaStore.loadIfNeeded()
  loadAdmins()
})
</script>

<style scoped>
.admin-admins {
  max-width: 1200px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

/* 穿梭器数据加载中的骨架屏容器 */
.transfer-loading {
  padding: 12px;
}
</style>
