package com.leang.authservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AuthRequest(
        @Email
        @NotNull
        @NotBlank
        @Schema(
                description = "Gmail",
                example = "Johnwick@gmail.com"
        )
        String email,
        @NotNull
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character"
        )
        @Schema(
                description = "User password (must be strong)",
                example = "StrongP@ssw0rd"
        )
        String password
) {
}
