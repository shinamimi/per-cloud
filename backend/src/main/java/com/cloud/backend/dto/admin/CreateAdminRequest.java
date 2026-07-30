package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

@Data
public class CreateAdminRequest {
    private String username;
    private String password;
    private String email;
    private String nickname;
    private Role role;
}