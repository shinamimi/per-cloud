<!--
  AddMemberDialog —— 团队成员添加弹窗。
  来源（docs/team-module.md §129/§134）：邀请成员可从好友列表勾选，也支持搜索用户添加；
  底部提供"去好友界面"入口（先添加好友，方便后续邀请）。
  已在团队中的用户标记"已在团队"并禁用。
-->
<template>
  <el-dialog
    v-model="visible"
    :title="`添加成员 - ${teamName}`"
    width="560px"
    draggable
    @open="loadFriends"
  >
    <el-tabs v-model="tab" type="border-card">
      <!-- 好友勾选 -->
      <el-tab-pane label="从好友选择" name="friends">
        <div v-loading="friendsLoading" class="pick-body">
          <el-empty v-if="!friendsLoading && friends.length === 0" description="暂无好友，可先去好友页添加" :image-size="60" />
          <el-checkbox-group v-else v-model="checkedUserIds" class="pick-list">
            <el-checkbox
              v-for="f in friends"
              :key="f.userId"
              :value="f.userId"
              :disabled="isMember(f.userId)"
              class="pick-row"
            >
              <span class="pick-name">{{ f.nickname || f.username }}</span>
              <span class="pick-sub">{{ f.username }}</span>
              <el-tag v-if="isMember(f.userId)" type="info" size="small" class="pick-tag">已在团队</el-tag>
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </el-tab-pane>

      <!-- 搜索添加 -->
      <el-tab-pane label="搜索用户" name="search">
        <div class="search-bar">
          <el-input v-model="keyword" placeholder="输入用户名/昵称搜索" clearable @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
        <div v-loading="searchLoading" class="pick-body">
          <el-empty v-if="!searchLoading && searched && results.length === 0" description="未找到匹配的用户" :image-size="60" />
          <div v-else-if="results.length > 0" class="pick-list">
            <div v-for="u in results" :key="u.userId" class="pick-row result-row">
              <el-avatar :size="30" :src="u.avatar || undefined">
                {{ (u.nickname || u.username)[0] }}
              </el-avatar>
              <span class="pick-name">{{ u.nickname || u.username }}</span>
              <span class="pick-sub">{{ u.username }}</span>
              <el-button
                size="small"
                type="primary"
                :disabled="isMember(u.userId)"
                @click="handleInviteOne(u)"
              >
                {{ isMember(u.userId) ? '已在团队' : '邀请' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="footer-row">
        <el-button link type="primary" @click="goFriends">
          <el-icon><User /></el-icon>
          <span>去好友界面添加好友</span>
        </el-button>
        <div class="footer-right">
          <el-button @click="visible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="checkedUserIds.length === 0"
            :loading="inviting"
            @click="handleInvite"
          >
            邀请所选（{{ checkedUserIds.length }}）
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { inviteMembers } from '@/api/team'
import { getFriendList, searchUsers } from '@/api/friend'
import type { TeamMember } from '@/types/team'
import type { FriendUser } from '@/types/friend'

const props = defineProps<{
  visible: boolean
  teamId: number
  teamName: string
  members: TeamMember[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  done: []
}>()

const router = useRouter()

const visible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const tab = ref('friends')
const friends = ref<FriendUser[]>([])
const friendsLoading = ref(false)
const checkedUserIds = ref<number[]>([])
const keyword = ref('')
const results = ref<FriendUser[]>([])
const searched = ref(false)
const searchLoading = ref(false)
const inviting = ref(false)

const memberIds = computed(() => new Set(props.members.map((m) => m.userId)))

function isMember(userId: number): boolean {
  return memberIds.value.has(userId)
}

async function loadFriends() {
  tab.value = 'friends'
  checkedUserIds.value = []
  friendsLoading.value = true
  try {
    friends.value = await getFriendList()
  } finally {
    friendsLoading.value = false
  }
}

async function handleSearch() {
  const k = keyword.value.trim()
  if (!k) return
  searched.value = true
  searchLoading.value = true
  try {
    results.value = await searchUsers(k)
  } finally {
    searchLoading.value = false
  }
}

async function handleInviteOne(u: FriendUser) {
  await inviteMembers(props.teamId, { userIds: [u.userId] })
  ElMessage.success(`已邀请 ${u.nickname || u.username}`)
  results.value = []
  emit('done')
}

async function handleInvite() {
  if (checkedUserIds.value.length === 0) return
  inviting.value = true
  try {
    await inviteMembers(props.teamId, { userIds: checkedUserIds.value })
    ElMessage.success(`已邀请 ${checkedUserIds.value.length} 位好友`)
    visible.value = false
    emit('done')
  } finally {
    inviting.value = false
  }
}

function goFriends() {
  visible.value = false
  router.push('/friends')
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.pick-body {
  min-height: 200px;
  max-height: 320px;
  overflow: auto;
}

.pick-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pick-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  margin-right: 0;
}

.pick-row:hover {
  background: #f5f7fa;
}

.pick-name {
  font-size: 13px;
  font-weight: 500;
}

.pick-sub {
  font-size: 12px;
  color: #909399;
}

.pick-tag {
  margin-left: auto;
}

.result-row {
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.footer-right {
  display: flex;
  gap: 8px;
}
</style>
