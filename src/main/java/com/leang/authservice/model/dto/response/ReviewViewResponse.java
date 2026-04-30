package com.leang.authservice.model.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewViewResponse(
        UUID id,
        UUID userId,
        String userName,
        String userImageUrl,
        UUID productId,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
