package com.leang.authservice.model.dto.request;


public record UpdateAppUserRequest(
        String username,
        String firstName,
        String lastName
) {}
