package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.request.UpdatePasswordRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class UserController extends BaseResponse {
    private final AppUserService appUserService;

    @Operation(summary = "Get user information")
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<AppUserResponse>> getCurrentUserProfile() {
        return responseEntity(true, "User profile retrieved successfully.", HttpStatus.OK, appUserService.getUserProfile());
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update user information")
    @PutMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> updateCurrentUserProfile(@RequestBody UpdateAppUserRequest updateAppUserRequest) {
        return responseEntity(true, "User profile updated successfully.", HttpStatus.OK, appUserService.updateCurrentUserProfile(updateAppUserRequest));
    }
    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AppUserResponse>> register(@RequestBody AppUserRequest appUserRequest) {
        return responseEntity(true, "User registered successfully.", HttpStatus.CREATED, appUserService.createUser(appUserRequest));
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change user password")
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<AppUserResponse>> changeUserPassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        appUserService.updateUserPassword(updatePasswordRequest);
        return responseEntity(true, "User password updated successfully.", HttpStatus.OK);
    }
}
