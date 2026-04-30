package com.leang.authservice.service;

import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserProfileRepository userProfileRepository;

    public Jwt jwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public String keycloakSub() {
        return jwt().getSubject();
    }

    public List<String> roles() {
        Jwt jwt = jwt();
        List<String> roles = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            realmRoles.forEach(r -> roles.add(String.valueOf(r)));
        }
        return roles;
    }

    public UserProfile ensureProfile() {
        Jwt jwt = jwt();
        return userProfileRepository.findByKeycloakId(jwt.getSubject())
                .map(existing -> {
                    existing.setEmail(jwt.getClaimAsString("email"));
                    existing.setUsername(jwt.getClaimAsString("preferred_username"));
                    if (existing.getFirstName() == null) {
                        existing.setFirstName(jwt.getClaimAsString("given_name"));
                    }
                    if (existing.getLastName() == null) {
                        existing.setLastName(jwt.getClaimAsString("family_name"));
                    }
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .keycloakId(jwt.getSubject())
                                .email(jwt.getClaimAsString("email"))
                                .username(jwt.getClaimAsString("preferred_username"))
                                .firstName(jwt.getClaimAsString("given_name"))
                                .lastName(jwt.getClaimAsString("family_name"))
                                .build()
                ));
    }

    /**
     * Prefer app-stored avatar; then OIDC/JWT picture; then optional imageUrl claim (e.g. Keycloak mapper).
     */
    public String resolveProfileImageUrl(UserProfile profile) {
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            return profile.getAvatarUrl().trim();
        }
        Jwt jwt = jwt();
        String picture = jwt.getClaimAsString("picture");
        if (picture != null && !picture.isBlank()) {
            return picture.trim();
        }
        String imageUrl = jwt.getClaimAsString("imageUrl");
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        return null;
    }
}
