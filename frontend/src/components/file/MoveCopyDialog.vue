<!--
  MoveCopyDialog —— 移动/复制对话框（支持单选/多选批量）。
  通过目录树选择目标目录（el-tree-select），确认后逐项调用对应接口。
  移动仅改数据库 parentId（MinIO 对象不动）；复制为同用户对象复制。
-->
<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'move' ? '移动' : '复制'"
    width="420px"
    :close-on-click-modal="false"
    draggable
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form label-width="80px">
      <el-form-item label="目标文件">
        <span v-if="targets.length === 1">{{ targets[0].name }}</span>
        <span v-else>已选 {{ targets.length }} 项</span>
      </el-form-item>
      <el-form-item label="目标目录">
        <el-tree-select
          v-model="targetParentId"
          :data="treeData"
          :props="TREE_PROPS"
          node-key="id"
          check-strictly
          default-expand-all
          placeholder="请选择目标目录（根目录）"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useFileStore } from '@/stores/file'
import { moveFile, copyFile } from '@/api/file'
import type { FileItem } from '@/types/file'

const props = defineProps<{
  visible: boolean
  targets: FileItem[]
  mode: 'move' | 'copy'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const fileStore = useFileStore()

const TREE_PROPS = { label: 'name', children: 'children' }

/** 目标目录（默认根目录）；选择器树数据带根节点 */
const targetParentId = ref<number>(0)

const treeData = computed(() => [{ id: 0, name: '全部文件（根目录）', children: fileStore.tree }])

const submitting = ref(false)

watch(
  () => props.visible,
  (open) => {
    if (open) {
      targetParentId.value = 0
      fileStore.loadTree()
    }
  },
)

async function handleConfirm() {
  if (props.targets.length === 0) return

  submitting.value = true
  try {
    if (props.mode === 'move') {
      await Promise.all(props.targets.map((f) => moveFile(f.id, { targetParentId: targetParentId.value })))
      ElMessage.success(`已移动 ${props.targets.length} 项`)
    } else {
      await Promise.all(props.targets.map((f) => copyFile(f.id, { targetParentId: targetParentId.value })))
      ElMessage.success(`已复制 ${props.targets.length} 项`)
    }
    emit('update:visible', false)
    fileStore.clearSelection()
    fileStore.refresh()
    fileStore.loadTree(true)
  } catch {
    // 错误已在拦截器中提示
  } finally {
    submitting.value = false
  }
}
</script>
