# 好友系统功能方案

## 一、功能边界

好友系统是通用关系层，独立于团队，供团队拉人、定向分享等场景复用。

| 功能 | 说明 | 本次 |
|------|------|:----:|
| 好友列表 / 搜索加好友 | 用户名/邮箱/手机号搜索（手机号未来绑定） | ✅ |
| 好友请求流程 | 双向确认：发请求 → 接受/拒绝 | ✅ |
| 删除好友 | 单向解除关系 | ✅ |
| 定向分享给好友 | 分享时可选好友为目标 | ✅ |
| 好友间通讯 | 不包含 | — |

## 二、关系形态

- **双向确认**：A 发请求 → B 接受才成为好友，有待处理请求列表
- 独立关系表（friendship），与团队（team_member）互不依赖
- 好友关系是"认识的人"关系层，团队拉人、定向分享都复用

## 三、接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friends` | 好友列表 |
| GET | `/api/friends/search?keyword=` | 搜索用户（用户名/邮箱/手机号） |
| POST | `/api/friends/requests` | 发送好友请求 |
| GET | `/api/friends/requests` | 待处理请求列表（收到的） |
| PUT | `/api/friends/requests/{id}/accept` | 接受请求 |
| PUT | `/api/friends/requests/{id}/reject` | 拒绝请求 |
| DELETE | `/api/friends/{userId}` | 删除好友 |

## 四、业务规则

### 4.1 好友请求

- 不能加自己
- 已是好友 / 已有待处理请求时不能重复发送
- 对方已拒绝后，可重新发送

### 4.2 好友状态机

```
发起请求 → 待接受（对方视角 pending）
待接受 → 接受 → 好友（ACTIVE）
待接受 → 拒绝 → 已拒绝（REJECTED，记录可重发）
好友 → 删除 → 解除（对方也可再发请求）
```

### 4.3 定向分享

- 定向分享对象：**好友 + 同团队成员**
- 分享接口校验目标人与分享者的关系（好友或同团队）

## 五、前端

| 路由 | 页面 |
|------|------|
| `/friends` | 好友页面：好友列表、待处理请求、搜索加好友 |

- axios：`src/api/friend.ts`
- 建团队时可选从好友列表勾选

## 六、数据模型

| 表 | 字段 | 说明 |
|----|------|------|
| t_friend_request | id, from_user_id, to_user_id, status(PENDING/ACCEPTED/REJECTED), created_at | 好友请求 |
| t_friendship | id, user_a_id, user_b_id, created_at | 好友关系（成对存储，查询便捷） |

## 七、需要做的事

1. `entity/FriendRequest.java`、`entity/Friendship.java` + Mapper
2. `service/friend/FriendService.java`：列表/搜索/请求/接受/拒绝/删除
3. `controller/FriendController.java`
4. `enums/FriendRequestStatus.java`
5. 前端 `/friends` 页面 + `api/friend.ts`
6. 定向分享接口校验好友/团队成员关系

## 八、变更范围

### 涉及文件
- `controller/FriendController.java`
- `service/friend/`：FriendService + impl
- `mapper/`：FriendRequestMapper、FriendshipMapper + xml
- `entity/`：FriendRequest、Friendship
- `enums/FriendRequestStatus.java`
- 分享模块：ShareService 校验目标关系
- `sql/`：t_friend_request、t_friendship 建表
- 前端：`views/friends/FriendsView.vue`、`api/friend.ts`

### 禁止修改
- 团队模块结构
- 现有认证/授权体系
