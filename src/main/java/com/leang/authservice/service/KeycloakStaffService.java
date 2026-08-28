package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.CreateStaffUserRequest;
import com.leang.authservice.model.dto.request.UpdateStaffUserRequest;
import com.leang.authservice.model.dto.response.AdminStaffUserResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;

public interface KeycloakStaffService {

    AdminStaffUserResponse createStaffUser(CreateStaffUserRequest request);

    ApiResponseWithPagination<AdminStaffUserResponse> listStaffUsers(int page, int size, String role, String search);

    AdminStaffUserResponse updateStaffUser(String keycloakUserId, UpdateStaffUserRequest request);
}
