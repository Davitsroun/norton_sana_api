package com.leang.authservice.model.dto.request;

public record AuthRequest(
        String email,
        String password
) {
}
