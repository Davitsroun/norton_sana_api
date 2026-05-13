package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.UserNotificationCreateRequest;
import com.leang.authservice.model.dto.request.UserNotificationUpdateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.UserNotificationResponse;
import com.leang.authservice.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-notifications")
@RequiredArgsConstructor
@Tag(name = "User notifications")
@SecurityRequirement(name = "bearerAuth")
public class UserNotificationController extends BaseResponse {

    private final UserNotificationService userNotificationService;

    @Operation(summary = "List my notifications")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserNotificationResponse>>> list() {
        return responseEntity(true, "Notifications retrieved successfully.", HttpStatus.OK, userNotificationService.listMine());
    }

    @Operation(summary = "Get one notification")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserNotificationResponse>> get(@PathVariable UUID id) {
        return responseEntity(true, "Notification retrieved successfully.", HttpStatus.OK, userNotificationService.getMine(id));
    }

    @Operation(summary = "Create a notification for myself")
    @PostMapping
    public ResponseEntity<ApiResponse<UserNotificationResponse>> create(@Valid @RequestBody UserNotificationCreateRequest request) {
        return responseEntity(true, "Notification created successfully.", HttpStatus.CREATED, userNotificationService.createMine(request));
    }

    @Operation(summary = "Update my notification (read flag, title, body)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserNotificationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserNotificationUpdateRequest request
    ) {
        return responseEntity(true, "Notification updated successfully.", HttpStatus.OK, userNotificationService.updateMine(id, request));
    }

    @Operation(summary = "Delete my notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userNotificationService.deleteMine(id);
        return responseEntity(true, "Notification deleted successfully.", HttpStatus.OK);
    }
}
