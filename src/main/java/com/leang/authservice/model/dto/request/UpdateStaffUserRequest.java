package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateStaffUserRequest(
        Boolean enabled,
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters with upper, lower, number, and special character"
        )
        String temporaryPassword,
        Boolean temporaryPasswordFlag
) {
}
