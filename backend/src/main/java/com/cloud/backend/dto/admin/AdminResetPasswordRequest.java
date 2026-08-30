package com.cloud.backend.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordRequest {

    /** 新密码：长度大于 8 位，且必须同时包含数字和英文 */
    @Size(min = 9, message = "密码长度必须大于 8 位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码必须同时包含数字和英文")
    private String newPassword;
}
