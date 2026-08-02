<!--
  DirectoryTree —— 目录树（左侧栏）。
  数据来自 fileStore.tree（GET /api/files/tree），
  点击节点进入目录，当前目录高亮；删除目录后需调用 refresh 重建。
-->
<template>
  <div class="directory-tree">
    <div
      class="tree-item tree-root"
      :class="{ active: fileStore.currentDirId === 0 }"
      @click="handleSelect(0)"
    >
      <el-icon><Folder /></el-icon>
      <span>查看根目录</span>
    </div>
    <el-tree
      :data="fileStore.tree"
      :props="TREE_PROPS"
      node-key="id"
      :default-expand-all="false"
      :expand-on-click-node="false"
      :highlight-current="true"
      :current-node-key="fileStore.currentDirId || undefined"
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon><Folder /></el-icon>
          <span class="tree-node-name">{{ data.name }}</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useFileStore } from '@/stores/file'
import type { FileTreeItem } from '@/types/file'

const fileStore = useFileStore()

const TREE_PROPS = { label: 'name', children: 'children' }

function handleNodeClick(node: FileTreeItem) {
  fileStore.navigate(node.id)
}

function handleSelect(dirId: number) {
  fileStore.navigate(dirId)
}

onMounted(() => {
  fileStore.loadTree()
})
</script>

<style scoped>
.directory-tree {
  min-height: 200px;
}

.tree-root {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 4px;
}

.tree-root:hover {
  background: #f5f7fa;
}

.tree-root.active {
  background: #ecf5ff;
  color: #409eff;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
}

.tree-node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
