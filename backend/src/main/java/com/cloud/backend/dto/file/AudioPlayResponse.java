package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 音乐播放地址响应 —— 扩展预留（file-module.md 十一）。
 * 播放器独立页接入时直接使用 url 直连 MinIO 播放。
 *
 * 修改指引：
 * - 【统一】修改 fileId          → Long fileId；播放文件 id，前端用于定位当前播放项；改名需同步前端 API 层与 Service 组装
 * - 【习惯】修改 name            → String name；音频文件名
 * - 【统一】修改 url             → String url；直连 MinIO 的预签名播放地址，有效期受管理员"下载链接有效期"配置影响，
 *                         过期后需重新请求 GET /api/files/{id}/play 获取新地址，前端勿做长缓存；改后需同步管理员"下载链接有效期"配置与前端缓存策略
 */
@Data
public class AudioPlayResponse {

    private Long fileId;

    private String name;

    private String url;
}
