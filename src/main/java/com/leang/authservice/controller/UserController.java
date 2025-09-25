package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController extends BaseResponse {
    private final AppUserService appUserService;

    @Operation(summary = "Get all user")
    @GetMapping
    public ResponseEntity<ApiResponse<ApiResponseWithPagination<AppUserResponse>>> getAllUser(@RequestParam(required = false) String username,
                                                                                              @RequestParam(required = false) String email,
                                                                                              @RequestParam(defaultValue = "1") Integer page,
                                                                                              @RequestParam(defaultValue = "10") Integer size) {
        return responseEntity(true, "All users retrieved successfully.", HttpStatus.OK, appUserService.findAllUsers(username, email, page, size));
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{user-id}")
    public ResponseEntity<ApiResponse<AppUserResponse>> getUserById(@PathVariable("user-id") String userId) {
        return responseEntity(true, "All users retrieved successfully.", HttpStatus.OK, appUserService.getUserById(userId));
    }


//            - **GET `/users/{id}`** → get user details.
}
