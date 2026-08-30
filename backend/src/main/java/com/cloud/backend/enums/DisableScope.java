package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum DisableScope {

    GLOBAL(1),
    USER(2);

    /** 【统一】改后需同步调用方判断逻辑（AdminFileServiceImpl 等按 getValue() 命中判定） */
    private final int value;

    DisableScope(int value) {
        this.value = value;
    }
}
