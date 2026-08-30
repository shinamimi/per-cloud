package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleChangeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "newRole 不能为空")
    private Role newRole;
}
