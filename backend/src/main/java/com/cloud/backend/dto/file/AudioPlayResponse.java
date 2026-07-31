package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 音乐播放地址响应 —— 扩展预留（file-module.md 十一）。
 * 播放器独立页接入时直接使用 url 直连 MinIO 播放。
 */
@Data
public class AudioPlayResponse {

    private Long fileId;

    private String name;

    private String url;
}
