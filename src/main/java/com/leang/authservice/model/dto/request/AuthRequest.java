package com.leang.authservice.model.dto.request;

public record AuthRequest(
        String username,
        String password
) {
}
