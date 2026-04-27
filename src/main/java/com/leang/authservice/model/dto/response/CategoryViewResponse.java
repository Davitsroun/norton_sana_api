package com.leang.authservice.model.dto.response;

import java.util.UUID;

public record CategoryViewResponse(
        UUID id,
        String code,
        String label
) {
}
