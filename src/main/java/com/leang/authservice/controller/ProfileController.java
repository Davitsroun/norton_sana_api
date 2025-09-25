package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProfileController extends BaseResponse {
    private final AppUserService appUserService;

    @Operation(summary = "Get current user profile")
    @GetMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> getCurrentUserProfile() {
        return responseEntity(true, "Current user profile retrieved successfully.", HttpStatus.OK, appUserService.getUserProfile());
    }

    @Operation(summary = "Update current user profile")
    @PutMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> updateCurrentUserProfile(@RequestBody UpdateAppUserRequest updateAppUserRequest) {
        return responseEntity(true, "Current user profile updated successfully.", HttpStatus.OK, appUserService.updateCurrentUserProfile(updateAppUserRequest));
    }

    @Operation(summary = "Delete current user profile")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> deleteCurrentUserProfile() {
        appUserService.deleteCurrentUserProfile();
        return responseEntity(true, "Current user deleted successfully.", HttpStatus.OK);
    }
}
