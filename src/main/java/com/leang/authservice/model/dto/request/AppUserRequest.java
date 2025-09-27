package com.leang.authservice.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppUserRequest(
        @NotNull
        @NotBlank
        String username,
        @NotNull
        @NotBlank
        String email,
        @NotNull
        @NotBlank
        String firstName,
        @NotNull
        @NotBlank
        String lastName,
        @NotNull
        @NotBlank
        String password,
        String imageUrl
) {}
