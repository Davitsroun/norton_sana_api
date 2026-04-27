package com.leang.authservice.model.dto.request;

public record MePatchRequest(
        String firstName,
        String lastName,
        String phone,
        String avatarUrl
) {
}
