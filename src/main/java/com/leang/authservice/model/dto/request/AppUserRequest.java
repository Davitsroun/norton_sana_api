package com.leang.authservice.model.dto.request;


public record AppUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        String password
) {}
