package com.leang.authservice.service;


import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.request.UpdatePasswordRequest;
import com.leang.authservice.model.dto.response.AppUserResponse;

public interface AppUserService {

    AppUserResponse createUser(AppUserRequest appUserRequest);

    AppUserResponse getUserProfile();

    AppUserResponse updateCurrentUserProfile(UpdateAppUserRequest updateAppUserRequest);

    void updateUserPassword(UpdatePasswordRequest updatePasswordRequest);
}
