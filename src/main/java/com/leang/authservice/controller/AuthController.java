package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.AuthResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.AppUserService;
import com.leang.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auths")
@Tag(name = "Authentication")
public class AuthController extends BaseResponse {
    private final AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return responseEntity(true, "User Login successfully.", HttpStatus.OK, "Log out success.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestParam String refreshToken) {
        return responseEntity(true, "User Login successfully.", HttpStatus.OK, authService.refresh(refreshToken));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest authRequest) {
        return responseEntity(true, "User Login successfully.", HttpStatus.OK, authService.login(authRequest));
    }
}
