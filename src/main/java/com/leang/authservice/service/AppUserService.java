package com.leang.authservice.service;


import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.model.dto.response.AuthResponse;

public interface AppUserService {

    AppUserResponse createUser(AppUserRequest appUserRequest);

    ApiResponseWithPagination<AppUserResponse> findAllUsers(String username, String email, Integer page, Integer size);

    AppUserResponse getUserById(String userId);

    AuthResponse login(AuthRequest authRequest);

    AppUserResponse getUserProfile();

    AppUserResponse updateCurrentUserProfile(UpdateAppUserRequest updateAppUserRequest);

    void deleteCurrentUserProfile();
}
