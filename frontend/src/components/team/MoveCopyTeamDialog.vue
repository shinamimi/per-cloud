<!--
  MoveCopyTeamDialog —— 团队文件移动/复制目标选择。
  复用团队目录树（GET /api/teams/{id}/files/tree），排除目标自身及其子树；
  确认后调用 moveTeamFile / copyTeamFile（团队命名空间自动去重命名）。
-->
<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'move' ? '移动文件' : '复制文件'"
    width="440px"
    draggable
    @open="loadTree"
  >
    <div class="target-summary">
      {{ mode === 'move' ? '移动' : '复制' }}「{{ target?.name }}」到：
    </div>
    <el-tree
      v-loading="loading"
      :data="tree"
      :props="{ label: 'name', children: 'children' }"
      node-key="id"
      :default-expand-all="true"
      highlight-current
      :current-node-key="currentId"
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon><Folder /></el-icon>
          <span>{{ data.name }}</span>
        </span>
      </template>
    </el-tree>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="currentId === null" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import { copyTeamFile, getTeamFileTree, moveTeamFile } from '@/api/team'
import type { FileItem, FileTreeItem } from '@/types/file'

const props = defineProps<{
  visible: boolean
  teamId: number
  target: FileItem | null
  mode: 'move' | 'copy'
  /** 排除目标自身及其子树（不能移动到自身内部） */
  excludeId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  done: []
}>()

const visible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const loading = ref(false)
const tree = ref<FileTreeItem[]>([])
const currentId = ref<number | null>(null)

async function loadTree() {
  loading.value = true
  currentId.value = null
  try {
    const all = await getTeamFileTree(props.teamId)
    tree.value = prune(all, new Set<number>(props.excludeId ? collectIds(all, props.excludeId) : []))
  } finally {
    loading.value = false
  }
}

function collectIds(nodes: FileTreeItem[], id: number): number[] {
  const ids: number[] = []
  for (const node of nodes) {
    if (node.id === id) {
      ids.push(node.id)
      flatten(node.children, ids)
    } else {
      ids.push(...collectIds(node.children, id))
    }
  }
  return ids
}

function flatten(nodes: FileTreeItem[], out: number[]) {
  for (const node of nodes) {
    out.push(node.id)
    flatten(node.children, out)
  }
}

/** 剪掉目标自身子树（排除集合） */
function prune(nodes: FileTreeItem[], excluded: Set<number>): FileTreeItem[] {
  return nodes
    .filter((n) => !excluded.has(n.id))
    .map((n) => ({ ...n, children: prune(n.children, excluded) }))
}

function handleNodeClick(data: FileTreeItem) {
  currentId.value = data.id
}

async function handleConfirm() {
  if (currentId.value === null || !props.target) return
  if (props.mode === 'move') {
    await moveTeamFile(props.teamId, props.target.id, { targetParentId: currentId.value })
    ElMessage.success('移动成功')
  } else {
    await copyTeamFile(props.teamId, props.target.id, { targetParentId: currentId.value })
    ElMessage.success('复制成功')
  }
  visible.value = false
  emit('done')
}
</script>

<style scoped>
.target-summary {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
