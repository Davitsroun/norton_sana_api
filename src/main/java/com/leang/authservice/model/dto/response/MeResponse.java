package com.leang.authservice.model.dto.response;

import java.util.List;

public record MeResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String username,
        String imageUrl,
        List<String> roles,
        boolean isAdmin
) {
}
