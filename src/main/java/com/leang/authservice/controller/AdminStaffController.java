package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.CreateStaffUserRequest;
import com.leang.authservice.model.dto.request.UpdateStaffUserRequest;
import com.leang.authservice.model.dto.response.AdminStaffUserResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.KeycloakStaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin user management")
@SecurityRequirement(name = "bearerAuth")
public class AdminStaffController extends BaseResponse {

    private final KeycloakStaffService keycloakStaffService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminStaffUserResponse>> createStaffUser(
            @Valid @RequestBody CreateStaffUserRequest request
    ) {
        AdminStaffUserResponse created = keycloakStaffService.createStaffUser(request);
        return responseEntity(true, "Staff user created successfully.", HttpStatus.CREATED, created);
    }

    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<AdminStaffUserResponse>> listStaffUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "search", required = false) String search
    ) {
        ApiResponseWithPagination<AdminStaffUserResponse> response =
                keycloakStaffService.listStaffUsers(page, size, role, search);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminStaffUserResponse>> updateStaffUser(
            @PathVariable("id") String keycloakUserId,
            @Valid @RequestBody UpdateStaffUserRequest request
    ) {
        AdminStaffUserResponse updated = keycloakStaffService.updateStaffUser(keycloakUserId, request);
        return responseEntity(true, "Staff user updated successfully.", HttpStatus.OK, updated);
    }
}
