package com.leang.authservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUserResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
}
