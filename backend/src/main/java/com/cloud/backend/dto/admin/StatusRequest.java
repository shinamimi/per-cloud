package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.UserStatus;
import lombok.Data;

@Data
public class StatusRequest {
    private UserStatus status;
}