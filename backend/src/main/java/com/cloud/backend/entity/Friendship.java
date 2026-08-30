package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Friendship {

    private Long id;
    private Long userAId;
    private Long userBId;
    private LocalDateTime createdAt;
}
