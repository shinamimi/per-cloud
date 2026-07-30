package com.cloud.backend.utils;

import com.cloud.backend.security.LoginUser;

public interface JwtTokenUtil {

    String generateToken(LoginUser loginUser);

    String getUsernameFromToken(String token);

    Integer getRoleFromToken(String token);

    boolean validateToken(String token);

    long getExpirationMs();
}
