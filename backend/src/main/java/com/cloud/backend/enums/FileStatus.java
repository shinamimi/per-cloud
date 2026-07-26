package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum FileStatus {

    DELETED(0),
    NORMAL(1);

    private final int value;

    FileStatus(int value) {
        this.value = value;
    }

    public static FileStatus fromValue(int value) {
        for (FileStatus status : FileStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}