package com.cloud.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DisabledObject {

    private Long id;
    private String fileHash;
    private Integer scope;
    private Long userId;
    private Long createdBy;
    private String reason;
    private LocalDateTime createdAt;
}
