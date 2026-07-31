<template>
  <!--
    AdminAdminView —— 管理员账户管理页面。
    仅 SUPER_ADMIN 可访问（后端 SecurityConfig 限制 /api/admin/admins/**），
    用于创建/删除 ADMIN 角色用户，或修改其角色。
  -->
  <div class="admin-admins">
    <h2 class="page-title">管理员管理</h2>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="openCreateDialog">创建管理员</el-button>
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
            <el-tag :type="row.role === 100 ? 'danger' : 'warning'" size="small">
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
            <el-option label="管理员" :value="20" />
            <el-option label="超级管理员" :value="100" />
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
          <el-tag :type="editRoleDialog.target?.role === 100 ? 'danger' : 'warning'" size="small">
            {{ roleLabel(editRoleDialog.target?.role || 20) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="editRoleDialog.newRole" style="width: 100%">
            <el-option label="管理员" :value="20" />
            <el-option label="超级管理员" :value="100" />
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
 * 此页面涉及的操作（创建/删除/修改角色）都需要 SUPER_ADMIN 权限，
 * 后端 SecurityConfig 通过 .hasRole("SUPER_ADMIN") 限制 /api/admin/admins/**，
 * 前端不做额外校验，权限不足的请求会被后端拦截并返回 403。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminAccounts,
  createAdminAccount,
  deleteAdminAccount,
  updateAdminRole,
} from '@/api/admin'
import type { AdminUserResponse } from '@/types/admin'
import { AdminRole, AdminRoleLabel } from '@/types/admin'

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

function roleLabel(role: number): string {
  return AdminRoleLabel[role as AdminRole] || '未知'
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
    role: AdminRole.ADMIN,
  },
})

function openCreateDialog() {
  createDialog.form = { username: '', password: '', email: '', nickname: '', role: AdminRole.ADMIN }
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
  newRole: AdminRole.ADMIN,
})

function openEditRoleDialog(row: AdminUserResponse) {
  editRoleDialog.target = row
  editRoleDialog.newRole = row.role as AdminRole
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

onMounted(loadAdmins)
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
  margin-bottom: 16px;
}
</style>
