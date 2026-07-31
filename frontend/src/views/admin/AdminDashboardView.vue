<template>
  <!--
    AdminDashboardView —— 管理后台仪表盘。
    展示系统概要数据：用户数、文件数、总存储量、配额使用率。
  -->
  <div class="admin-dashboard">
    <h2 class="page-title">仪表盘</h2>

    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statsCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="usage-card">
      <template #header>
        <span>存储使用概览</span>
      </template>
      <div class="usage-row">
        <span>已使用：{{ formatBytesAuto(stats.totalSize) }}</span>
        <span>总配额：{{ formatBytesAuto(stats.totalQuota) }}</span>
        <span>使用率：{{ stats.usagePercent.toFixed(1) }}%</span>
      </div>
      <el-progress
        :percentage="Math.min(Math.round(stats.usagePercent), 100)"
        :color="usageColor"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
/*
 * 仪表盘 —— 从后端获取统计数据并展示。
 *
 * 设计思路：
 * - 四个统计卡片使用 el-row + el-col 栅格布局响应式排列
 * - 存储使用进度条颜色随使用率变化（<60% 绿色，<80% 橙色，>=80% 红色）
 * - 容量展示使用 formatBytesAuto 自动选择单位（B/KB/MB/GB/TB），只读场景不提供单位选择
 */
import { ref, onMounted, computed } from 'vue'
import { getDashboardStats } from '@/api/admin/dashboard'
import type { AdminDashboardStats } from '@/types/admin'
import { formatBytesAuto } from '@/utils/format'

const stats = ref<AdminDashboardStats>({
  userCount: 0,
  fileCount: 0,
  totalSize: 0,
  totalQuota: 0,
  usagePercent: 0,
})

const statsCards = computed(() => [
  { label: '用户总数', value: stats.value.userCount },
  { label: '文件总数', value: stats.value.fileCount },
  { label: '存储总量', value: formatBytesAuto(stats.value.totalSize) },
  { label: '配额总量', value: formatBytesAuto(stats.value.totalQuota) },
])

const usageColor = computed(() => {
  const pct = stats.value.usagePercent
  if (pct < 60) return '#67c23a'
  if (pct < 80) return '#e6a23c'
  return '#f56c6c'
})

onMounted(async () => {
  try {
    stats.value = await getDashboardStats()
  } catch {
    // 请求失败时使用默认值，错误信息已在拦截器中提示
  }
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1200px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.stat-card {
  text-align: center;
  margin-bottom: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
  line-height: 1.4;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.usage-card {
  margin-top: 8px;
}

.usage-row {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}
</style>
