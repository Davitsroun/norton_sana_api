package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNotificationCreateRequest(
        @NotBlank @Size(max = 64) String type,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String body
) {
}
