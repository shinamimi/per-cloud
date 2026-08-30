package com.cloud.backend.enums;

import lombok.Getter;

@Getter
    public enum TargetType {

        /** 【统一】改后同步业务侧 @Log(target = TargetType.XXX) 引用处 */
        USER,
    FILE,
    SHARE,
    TEAM;
}