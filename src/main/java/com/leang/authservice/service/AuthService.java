package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest authRequest);
    void logout(String refreshToken);
    AuthResponse refresh(String refreshToken);

}
