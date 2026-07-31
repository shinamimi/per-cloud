<!--
  BreadcrumbNav —— 面包屑导航。
  从 fileStore.breadcrumb 渲染「根目录 → 当前目录」路径，点击节点跳转。
-->
<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item>
      <a class="breadcrumb-link" @click="handleNavigate(0)">全部文件</a>
    </el-breadcrumb-item>
    <el-breadcrumb-item v-for="node in fileStore.breadcrumb" :key="node.id">
      <a class="breadcrumb-link" @click="handleNavigate(node.id)">{{ node.name }}</a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { useFileStore } from '@/stores/file'

const fileStore = useFileStore()

function handleNavigate(dirId: number) {
  fileStore.navigateBreadcrumb(dirId)
}
</script>

<style scoped>
.breadcrumb-link {
  cursor: pointer;
  color: inherit;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #409eff;
}
</style>
