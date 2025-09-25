package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.model.dto.response.AuthResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auths")
public class AuthController extends BaseResponse {
    private final AppUserService appUserService;

    @Operation(summary = "Create a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AppUserResponse>> createAppUser(@RequestBody AppUserRequest appUserRequest) {
        return responseEntity(true, "User created successfully.", HttpStatus.CREATED, appUserService.createUser(appUserRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest authRequest) {
        return responseEntity(true, "User Login successfully.", HttpStatus.OK, appUserService.login(authRequest));
    }
}
