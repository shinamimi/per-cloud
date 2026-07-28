<template>
  <!-- 文件管理占位页面 —— 登录后的默认着陆页，后续替换为完整的 FilePage -->
  <div class="welcome">
    <h2>欢迎回来，{{ userStore.username }}</h2>
    <p>文件管理功能正在开发中...</p>
    <el-button type="danger" @click="handleLogout">退出登录</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout().then(() => {
    router.push({ name: 'Login' })
  })
}
</script>

<style scoped>
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #606266;
}

.welcome h2 {
  margin-bottom: 8px;
}
</style>
