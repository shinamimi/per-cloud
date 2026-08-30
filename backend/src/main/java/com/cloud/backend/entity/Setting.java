package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Setting {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
