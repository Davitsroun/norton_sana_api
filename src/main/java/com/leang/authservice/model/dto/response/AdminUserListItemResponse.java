package com.leang.authservice.model.dto.response;

import java.time.Instant;

public record AdminUserListItemResponse(
        String id,
        String name,
        String email,
        Instant joinedAt,
        long orderCount,
        String status,
        String role,
        String avatarUrl
) {
}
