<!--
  FriendsView —— 好友管理页面（/friends）。
  三块：好友请求（待处理/已发送）、好友列表、发现用户（搜索 + 发送请求）。
  好友关系双向确认 —— 对方接受后才建立好友；删除为单向解除。
-->
<template>
  <div class="friends-view">
    <el-card shadow="never">
      <div class="card-header">
        <span class="card-title">好友</span>
        <el-input
          v-model="keyword"
          placeholder="搜索用户名/昵称"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="searchResults = []"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchResults.length > 0" class="section">
        <div class="section-title">搜索结果</div>
        <div class="user-grid">
          <div v-for="u in searchResults" :key="u.userId" class="user-card">
            <el-avatar :size="40" :src="u.avatar || undefined">
              {{ (u.nickname || u.username)[0] }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ u.nickname || u.username }}</div>
              <div class="user-sub">{{ u.username }} · {{ u.email }}</div>
            </div>
            <el-button
              v-if="u.relation === 'FRIEND'"
              type="success"
              size="small"
              disabled
            >
              已是好友
            </el-button>
            <el-button
              v-else-if="u.relation === 'TO_THEM'"
              size="small"
              disabled
            >
              对方已向你发起请求
            </el-button>
            <el-button v-else size="small" type="primary" @click="handleAddFriend(u)">
              加好友
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <div class="columns">
      <!-- 待处理请求 -->
      <el-card shadow="never" class="column-card">
        <div class="card-header">
          <span class="card-title">
            好友请求
            <el-badge v-if="pendingRequests.length > 0" :value="pendingRequests.length" />
          </span>
        </div>
        <el-empty v-if="pendingRequests.length === 0" description="暂无待处理请求" :image-size="60" />
        <div v-else class="request-list">
          <div v-for="req in pendingRequests" :key="req.requestId" class="request-item">
            <el-avatar :size="36" :src="req.fromAvatar || undefined">
              {{ (req.fromNickname || req.fromUsername)[0] }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ req.fromNickname || req.fromUsername }}</div>
              <div class="user-sub">{{ req.fromUsername }}</div>
            </div>
            <el-button size="small" type="success" @click="handleAccept(req)">接受</el-button>
            <el-button size="small" @click="handleReject(req)">拒绝</el-button>
          </div>
        </div>
      </el-card>

      <!-- 好友列表 -->
      <el-card shadow="never" class="column-card">
        <div class="card-header">
          <span class="card-title">我的好友（{{ friends.length }}）</span>
        </div>
        <el-empty v-if="friends.length === 0" description="还没有好友，去搜索添加吧" :image-size="60" />
        <div v-else class="user-grid">
          <div v-for="f in friends" :key="f.userId" class="user-card">
            <el-avatar :size="40" :src="f.avatar || undefined">
              {{ (f.nickname || f.username)[0] }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ f.nickname || f.username }}</div>
              <div class="user-sub">{{ f.username }}</div>
            </div>
            <el-button size="small" type="danger" plain @click="handleDelete(f)">
              删除
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  acceptFriendRequest,
  deleteFriend,
  getFriendList,
  getFriendRequests,
  rejectFriendRequest,
  searchUsers,
  sendFriendRequest,
} from '@/api/friend'
import type { FriendRequest, FriendUser } from '@/types/friend'

const keyword = ref('')
const searchResults = ref<FriendUser[]>([])
const friends = ref<FriendUser[]>([])
const pendingRequests = ref<FriendRequest[]>([])

async function handleSearch() {
  const k = keyword.value.trim()
  if (!k) return
  searchResults.value = await searchUsers(k)
  if (searchResults.value.length === 0) ElMessage.info('未找到匹配的用户')
}

async function handleAddFriend(u: FriendUser) {
  await sendFriendRequest({ toUserId: u.userId })
  ElMessage.success(`已向 ${u.nickname || u.username} 发送好友请求`)
}

async function handleAccept(req: FriendRequest) {
  await acceptFriendRequest(req.requestId)
  ElMessage.success('已添加好友')
  await Promise.all([loadFriends(), loadRequests()])
}

async function handleReject(req: FriendRequest) {
  await rejectFriendRequest(req.requestId)
  ElMessage.success('已拒绝')
  await loadRequests()
}

async function handleDelete(f: FriendUser) {
  try {
    await ElMessageBox.confirm(`确定删除好友「${f.nickname || f.username}」吗？`, '删除好友', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteFriend(f.userId)
  ElMessage.success('已删除')
  await loadFriends()
}

async function loadFriends() {
  friends.value = await getFriendList()
}

async function loadRequests() {
  pendingRequests.value = (await getFriendRequests('PENDING')).filter(
    (r) => r.status === 'PENDING',
  )
}

onMounted(() => {
  void Promise.all([loadFriends(), loadRequests()])
})
</script>

<style scoped>
.friends-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  flex: 1;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: start;
}

.column-card {
  min-height: 260px;
}

.section {
  margin-bottom: 8px;
}

.section-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.user-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-sub {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
</style>
