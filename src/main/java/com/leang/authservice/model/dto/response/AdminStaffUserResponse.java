package com.leang.authservice.model.dto.response;

import java.time.Instant;

public record AdminStaffUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean enabled,
        Instant createdAt,
        String avatarUrl
) {
}
