package com.leang.authservice.model.dto.request;


public record UpdateAppUserRequest(
        String username,
        String email,
        String firstName,
        String lastName
) {}
