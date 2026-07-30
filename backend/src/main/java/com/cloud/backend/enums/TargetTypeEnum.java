package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 操作日志的操作目标类型 —— 标记本次操作关联什么类型的对象。
 * 配合 operation_log.target_id 显式操作对象 ID，方便审计追溯。
 */
@Getter
public enum TargetTypeEnum {

    USER,
    FILE,
    SHARE;
}