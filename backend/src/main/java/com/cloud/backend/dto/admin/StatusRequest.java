package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.UserStatusEnum;
import lombok.Data;

@Data
public class StatusRequest {
    private UserStatusEnum status;
}