package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.RoleEnum;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    private RoleEnum role;
}