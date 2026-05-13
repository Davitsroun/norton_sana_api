package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.Size;

public record UserNotificationUpdateRequest(
        Boolean read,
        @Size(max = 255) String title,
        @Size(max = 2000) String body
) {
}
