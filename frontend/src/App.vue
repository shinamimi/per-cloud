<template>
  <!--
    App 根组件 —— 根据路由 meta.layout 切换布局。
    认证页面（login/register/forgot-password）使用 AuthLayout（居中卡片式），
    业务页面（files/profile/etc）使用 MainLayout（侧边栏+顶栏+内容区）。
  -->
  <component :is="layout">
    <router-view />
  </component>
</template>

<script setup lang="ts">
/*
 * 启动时尝试从 localStorage 恢复登录态。
 * 刷新页面时 Pinia 状态丢失，但 token/user 持久化在 localStorage 中，
 * 通过 userStore.loadFromStorage() 恢复，避免每次刷新都跳转到登录页。
 */
import { onMounted } from 'vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AuthLayout from '@/layout/AuthLayout.vue'
import MainLayout from '@/layout/MainLayout.vue'

const route = useRoute()
const userStore = useUserStore()

const layout = computed(() => {
  return (route.meta.layout === 'auth') ? AuthLayout : MainLayout
})

onMounted(() => {
  userStore.loadFromStorage()
})
</script>

<style>
/*
 * 全局基础样式重置 —— 确保所有页面 margin/padding 统一，方便 Element Plus 的 el-container 等布局组件正确撑满视口。
 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
