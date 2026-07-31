<template>
  <!--
    MainLayout —— 主业务页面布局。
    包含侧边栏 + 顶栏 + 内容区，已登录用户的所有页面共用此布局。
  -->
  <el-container class="main-layout">
    <el-header class="main-header">
      <div class="header-left">
        <span class="header-title">Cloud 云盘</span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.username || '用户' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container class="main-body">
      <el-aside width="220px" class="main-aside">
        <el-menu
          :default-active="currentRoute"
          router
          class="aside-menu"
        >
          <el-menu-item index="/files">
            <el-icon><Folder /></el-icon>
            <span>我的文件</span>
          </el-menu-item>

          <!--
            管理员菜单组 —— 仅在当前用户 role >= ADMIN 时显示。
            使用 v-if="userStore.isAdmin" 阻止非管理员看到管理入口。
          -->
          <el-menu-item v-if="userStore.isAdmin" index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <span>管理后台</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentRoute = computed(() => route.path)

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout().then(() => {
      router.push({ name: 'Login' })
    })
  } else if (command === 'profile') {
    // TODO: 跳转到个人中心
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 56px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.main-body {
  height: calc(100vh - 56px);
}

.main-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.aside-menu {
  border-right: none;
}

.main-content {
  background: #f5f7fa;
  padding: 20px;
}
</style>
