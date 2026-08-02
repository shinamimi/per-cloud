package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 对象级禁用范围（docs/admin-file-management.md 5.1 禁用粒度）。
 * GLOBAL=全站禁（按内容 hash，全站引用该内容的文件全部禁用）；
 * USER=仅用户（用户×内容，只禁该用户对该内容，其他用户不受影响）。
 */
@Getter
public enum DisableScope {

    GLOBAL(1),
    USER(2);

    private final int value;

    DisableScope(int value) {
        this.value = value;
    }
}
