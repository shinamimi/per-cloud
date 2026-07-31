# 穿梭器组件（Transfer.vue）功能文档

## 一、定位

通用工具组件，不属于任何页面功能。用于批量调整"成员/角色"归属：左列候选集合，右列已选集合，移动即变更。

- 位置：`src/components/common/Transfer.vue`
- 数据：**由父组件传入**，组件不自行请求
- 角色选项：从字典接口 role 组过滤（排除 USER 和 SUPER_ADMIN，仅剩"管理员/超级管理员"两档）

## 二、Props / Events

| 类型 | 名称 | 说明 |
|------|------|------|
| prop | `candidates` | 候选列表（左列），`[{ id, username, nickname }]` |
| prop | `selected` | 已选列表（右列），同上结构 |
| prop | `roleOptions` | 可选的角色档位，来自字典 role 组过滤后 |
| event | `confirm` | 确认时触发，携带批量变更数组 |
| event | `cancel` | 取消关闭 |

## 三、交互规则

| 操作 | 行为 |
|------|------|
| 全局角色选择器 | 弹窗顶部选择目标角色（管理员/超级管理员），默认管理员 |
| 左列 → 右列 | 移入，设为所选角色（后端 OPERATOR / ADMIN） |
| 右列 → 左列 | 移出，降级为普通用户（USER） |
| 确认 | 一次提交全部变更，批量原子处理 |

## 四、后端接口契约

### 4.1 候选列表

```
GET /api/admin/admins/candidates
```

- 返回可设为管理员的用户（排除已是 ADMIN / SUPER_ADMIN）
- 由父组件（管理员管理页）调用后传入组件

### 4.2 批量变更

```
PUT /api/admin/admins/batch
```

请求体统一为变更项数组，降级也传目标角色：

```json
[
  { "userId": 1, "newRole": "ADMIN" },
  { "userId": 2, "newRole": "OPERATOR" },
  { "userId": 3, "newRole": "USER" }
]
```

- `newRole` 取值：`OPERATOR` / `ADMIN`（设管理员）、`USER`（降级）
- 前端穿梭器只显示"管理员/超级管理员"两档，`USER` 档仅用于降级场景，不在选择器中出现
- 后端校验：仅 SUPER_ADMIN 可调用

## 五、变更范围

### 涉及文件
- 前端新增 `src/components/common/Transfer.vue`
- 前端 `AdminAdminView.vue` 新增"添加管理员"入口
- 前端新增 `src/api/admin/admin.ts` 中 candidates / batch 方法
- 后端 AdminAccountController 新增两个端点
- 后端 MetaService role 组返回混淆 label

### 相关文档
- 前端统一化规范：`docs/frontend-standard.md`
