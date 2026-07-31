<template>
  <!--
    Transfer —— 通用穿梭器组件（docs/component-transfer.md）。
    用于批量调整"成员/角色"归属：左列候选集合，右列已选集合，移动即变更。

    交互规则：
    - 全局角色选择器：顶部选择目标角色（管理员/超级管理员），默认第一档
    - 左列 → 右列：移入，设为所选角色
    - 右列 → 左列：移出，降级为普通用户（USER）
    - 确认：一次提交全部变更，携带变更数组 [{ userId, newRole }]
  -->
  <div class="transfer">
    <!-- 顶部：全局角色选择器 -->
    <div class="transfer-header">
      <span class="transfer-label">目标角色</span>
      <el-select v-model="targetRole" size="small" style="width: 160px">
        <el-option
          v-for="opt in roleOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <span class="transfer-hint">移入的用户将设为该角色</span>
    </div>

    <!-- 中部：左右两列 + 移动按钮 -->
    <div class="transfer-body">
      <div class="transfer-col">
        <div class="col-title">候选用户（{{ leftList.length }}）</div>
        <el-scrollbar height="240px">
          <el-checkbox-group v-model="leftChecked" class="col-list">
            <el-checkbox
              v-for="item in leftList"
              :key="item.id"
              :value="item.id"
              class="col-item"
            >
              {{ item.username }}{{ item.nickname ? `（${item.nickname}）` : '' }}
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="leftList.length === 0" description="暂无候选用户" :image-size="60" />
        </el-scrollbar>
      </div>

      <div class="transfer-actions">
        <el-button size="small" :disabled="leftChecked.length === 0" @click="moveRight">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
        <el-button size="small" :disabled="rightChecked.length === 0" @click="moveLeft">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
      </div>

      <div class="transfer-col">
        <div class="col-title">已选管理员（{{ rightList.length }}）</div>
        <el-scrollbar height="240px">
          <el-checkbox-group v-model="rightChecked" class="col-list">
            <el-checkbox
              v-for="item in rightList"
              :key="item.id"
              :value="item.id"
              class="col-item"
            >
              {{ item.username }}{{ item.nickname ? `（${item.nickname}）` : '' }}
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="rightList.length === 0" description="暂无管理员" :image-size="60" />
        </el-scrollbar>
      </div>
    </div>

    <!-- 底部：取消 / 确认 -->
    <div class="transfer-footer">
      <span v-if="pendingChanges.length > 0" class="pending-hint">
        共 {{ pendingChanges.length }} 项变更
      </span>
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :disabled="pendingChanges.length === 0" @click="handleConfirm">
        确认
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/*
 * 穿梭器实现原理：
 * - 组件不自行请求数据，candidates / selected 由父组件传入（数据归父组件）
 * - 组件内部维护两个本地副本（leftList / rightList），移动时同步记录变更到 Map
 * - changes Map 以 userId 为 key、newRole 为 value：
 *   移入 → targetRole（OPERATOR/ADMIN）；移出 → 'USER'（降级）
 * - 确认时把 Map 拍平成 [{ userId, newRole }] 数组 emit 给父组件
 *
 * 为什么用 Map 而不是数组？
 * 同一用户可能被反复移入/移出，Map 天然去重，最终只提交最后一次状态，
 * 避免对同一用户产生冲突的变更项。
 */
import { ref, reactive, computed } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { AdminRoleChange, RoleKey } from '@/types/admin'

interface TransferItem {
  id: number
  username: string
  nickname?: string | null
}

interface RoleOption {
  value: string
  label: string
}

const props = defineProps<{
  /** 候选列表（左列），由父组件从 GET /api/admin/admins/candidates 获取后传入 */
  candidates: TransferItem[]
  /** 已选列表（右列），当前管理员列表 */
  selected: TransferItem[]
  /** 可选角色档位，来自字典 role 组过滤（排除 USER 和 SUPER_ADMIN） */
  roleOptions: RoleOption[]
}>()

const emit = defineEmits<{
  /** 确认时触发，携带批量变更数组 [{ userId, newRole }] */
  (e: 'confirm', changes: AdminRoleChange[]): void
  /** 取消关闭 */
  (e: 'cancel'): void
}>()

/** 全局目标角色 —— 默认第一档（通常为"管理员"） */
const targetRole = ref(props.roleOptions[0]?.value ?? '')

/** 左列本地副本 */
const leftList = ref<TransferItem[]>(props.candidates.map((c) => ({ ...c })))
/** 右列本地副本 */
const rightList = ref<TransferItem[]>(props.selected.map((s) => ({ ...s })))

/** 左列勾选中的用户 id */
const leftChecked = ref<number[]>([])
/** 右列勾选中的用户 id */
const rightChecked = ref<number[]>([])

/** 变更记录：userId -> newRole（Map 天然去重，只保留最终状态） */
const changes = reactive(new Map<number, RoleKey>())

/** 待提交变更数组（用于底部提示条和确认按钮禁用） */
const pendingChanges = computed(() =>
  Array.from(changes.entries()).map(([userId, newRole]) => ({ userId, newRole })),
)

/** 左列 → 右列：移入，设为所选角色 */
function moveRight() {
  const moving = leftList.value.filter((item) => leftChecked.value.includes(item.id))
  rightList.value.push(...moving)
  leftList.value = leftList.value.filter((item) => !leftChecked.value.includes(item.id))
  for (const item of moving) {
    changes.set(item.id, targetRole.value as RoleKey)
  }
  leftChecked.value = []
}

/** 右列 → 左列：移出，降级为普通用户（USER） */
function moveLeft() {
  const moving = rightList.value.filter((item) => rightChecked.value.includes(item.id))
  leftList.value.push(...moving)
  rightList.value = rightList.value.filter((item) => !rightChecked.value.includes(item.id))
  for (const item of moving) {
    changes.set(item.id, 'USER')
  }
  rightChecked.value = []
}

/** 确认：拍平变更 Map 并 emit，由父组件调用批量接口 */
function handleConfirm() {
  emit(
    'confirm',
    Array.from(changes.entries()).map(([userId, newRole]) => ({ userId, newRole })),
  )
}
</script>

<style scoped>
.transfer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.transfer-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.transfer-label {
  font-size: 14px;
  color: #606266;
}

.transfer-hint {
  font-size: 12px;
  color: #909399;
}

.transfer-body {
  display: flex;
  gap: 12px;
}

.transfer-col {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px;
}

.col-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  padding: 4px 8px 8px;
}

.col-list {
  display: flex;
  flex-direction: column;
  padding: 0 8px;
}

.col-item {
  margin-right: 0;
  height: 32px;
}

.transfer-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.transfer-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.pending-hint {
  font-size: 12px;
  color: #e6a23c;
}
</style>
