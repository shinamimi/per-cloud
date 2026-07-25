package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String email;

    private String nickname;

    private String avatar;

    private Integer role;

    private Long quota;

    private Long usedSpace;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}