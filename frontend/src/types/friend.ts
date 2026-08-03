/*
 * 好友模块类型定义 —— 对应后端 FriendController（/api/friends）与 dto/friend/*。
 *
 * 设计依据：
 * - 好友关系双向确认：发送请求 → 对方接受后建立双向好友
 * - relation 字段标注双方关系（NONE/FRIEND/TO_THEM），供搜索列表展示"加好友/聊天/已是好友"
 * - isFriendOrTeamMate 复用：定向分享、团队拉人的候选源
 */

/** 关系标注 —— FriendSearchResponse.relation */
export type FriendRelation = 'NONE' | 'FRIEND' | 'TO_THEM'

/** 好友请求状态 —— 对应后端 FriendRequestStatus 枚举 name */
export type FriendRequestStatusKey = 'PENDING' | 'ACCEPTED' | 'REJECTED'

/** 好友/搜索用户 —— 对应 FriendUserResponse / FriendSearchResponse */
export interface FriendUser {
  userId: number
  username: string
  nickname: string
  avatar: string
  email: string
  /** 搜索接口返回；好友列表无此字段 */
  relation?: FriendRelation
}

/** 好友请求 —— 对应 FriendRequestResponse */
export interface FriendRequest {
  requestId: number
  fromUserId: number
  fromUsername: string
  fromNickname: string
  fromAvatar: string
  status: FriendRequestStatusKey
  createdAt: string
}

/** 发送好友请求体 —— POST /api/friends/requests */
export interface FriendRequestCreate {
  toUserId: number
}
