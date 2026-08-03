/*
 * 好友模块 API —— 对应后端 FriendController（/api/friends）。
 *
 * relationship 语义：
 * - 搜索返回 relation（NONE/FRIEND/TO_THEM），TO_THEM 表示对方已向我发过请求
 * - 删除好友为单向删除（双向解除）
 */
import request from '@/utils/request'
import type {
  FriendRequest,
  FriendRequestCreate,
  FriendUser,
} from '@/types/friend'

/** 好友列表 —— GET /api/friends */
export function getFriendList(): Promise<FriendUser[]> {
  return request.get('/api/friends')
}

/** 搜索用户（前缀匹配，带关系标注）—— GET /api/friends/search?keyword= */
export function searchUsers(keyword: string): Promise<FriendUser[]> {
  return request.get('/api/friends/search', { params: { keyword } })
}

/** 发送好友请求 —— POST /api/friends/requests */
export function sendFriendRequest(data: FriendRequestCreate): Promise<void> {
  return request.post('/api/friends/requests', data)
}

/** 好友请求列表 —— GET /api/friends/requests（status 缺省返回全部） */
export function getFriendRequests(status?: string): Promise<FriendRequest[]> {
  return request.get('/api/friends/requests', { params: { status } })
}

/** 接受好友请求 —— PUT /api/friends/requests/{id}/accept */
export function acceptFriendRequest(id: number): Promise<void> {
  return request.put(`/api/friends/requests/${id}/accept`)
}

/** 拒绝好友请求 —— PUT /api/friends/requests/{id}/reject */
export function rejectFriendRequest(id: number): Promise<void> {
  return request.put(`/api/friends/requests/${id}/reject`)
}

/** 删除好友 —— DELETE /api/friends/{userId} */
export function deleteFriend(userId: number): Promise<void> {
  return request.delete(`/api/friends/${userId}`)
}
