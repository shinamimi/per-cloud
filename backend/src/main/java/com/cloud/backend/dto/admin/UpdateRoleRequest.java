package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    private Role role;
}