package com.cloud.backend.service.system;

import com.cloud.backend.dto.LoginRequest;
import com.cloud.backend.dto.LoginResponse;
import com.cloud.backend.dto.RegisterRequest;
import com.cloud.backend.dto.ResetPasswordRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ip);

    LoginResponse register(RegisterRequest request, String ip);

    void sendForgotPasswordCode(String email);

    void resetPassword(ResetPasswordRequest request);
}
