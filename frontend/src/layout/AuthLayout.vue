<template>
  <!--
    AuthLayout —— 认证页面专用布局。
    居中卡片式设计，适用于登录、注册、找回密码等页面。
  -->
  <div class="auth-layout">
    <div class="auth-frame">
      <div class="auth-container">
        <div class="auth-header">
          <h1 class="auth-title">Cloud 云盘</h1>
          <p class="auth-subtitle">企业级私有云存储服务</p>
        </div>
        <div class="auth-card">
          <router-view />
        </div>
      </div>
      <SiteFooter />
    </div>
  </div>
</template>

<script setup lang="ts">
/*
 * AuthLayout —— 认证页面专用布局。
 * 居中卡片式设计，适用于登录、注册、找回密码等页面。
 * 底部固定展示站点页脚（备案号）。
 */
import SiteFooter from '@/components/common/SiteFooter.vue'
</script>

<style scoped>
.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-frame {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.auth-container {
  width: 420px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-title {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
}

.auth-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.auth-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
</style>
