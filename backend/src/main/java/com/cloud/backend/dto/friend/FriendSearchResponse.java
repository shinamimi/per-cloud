package com.cloud.backend.dto.friend;

import lombok.Data;

/**
 * 搜索结果项 —— 加好友搜索用户。
 * relation：SELF（自己，不可加）/ FRIEND（已是好友）/ PENDING_SENT（已发请求待接受）/
 * PENDING_RECEIVED（对方已向你发请求）/ NONE（可添加）。
 *
 * 修改指引：
 * - 【统一】修改 userId          → Long userId；搜索结果用户 id；改名需同步前端加好友操作与 FriendService
 * - 【习惯】修改 username / nickname / avatar / email → 用户基本资料，仅展示用
 * - 【统一】修改 relation        → String relation；当前用户与该用户的关系：SELF / FRIEND / PENDING_SENT /
 *                         PENDING_RECEIVED / NONE；前端据此决定按钮状态（不可添加/已是好友/待验证/加好友），
 *                         新增关系值需同步 FriendService 判定逻辑与前端分支；改后需同步 FriendService 判定逻辑与前端按钮分支
 */
@Data
public class FriendSearchResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String relation;
}
