package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.MePatchRequest;
import com.leang.authservice.model.dto.response.MeResponse;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import com.leang.authservice.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final UserProfileRepository userProfileRepository;

    @GetMapping
    public MeResponse getCurrentUser() {
        UserProfile profile = currentUserService.ensureProfile();
        List<String> roles = currentUserService.roles();
        return new MeResponse(
                profile.getKeycloakId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getUsername(),
                profile.getAvatarUrl(),
                roles,
                roles.contains("admin")
        );
    }

    @PatchMapping
    public MeResponse patchCurrentUser(@RequestBody MePatchRequest request) {
        UserProfile profile = currentUserService.ensureProfile();
        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        userProfileRepository.save(profile);
        return getCurrentUser();
    }
}
