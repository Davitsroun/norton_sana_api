package com.leang.authservice.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAppUserRequest(
        @NotNull
        @Schema(
                description = "Username",
                example = "Johnwick123"
        )
        @NotBlank
        String username,

        @NotNull
        @NotBlank
        @Schema(
                description = "Firstname",
                example = "John"
        )
        String firstName,
        @NotNull
        @NotBlank
        @Schema(
                description = "Lastname",
                example = "Wick"
        )
        String lastName,
        String imageUrl
) {}
