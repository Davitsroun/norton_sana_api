package com.leang.authservice.service;

import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.leang.authservice.util.SecurityRoles;

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
        return new ArrayList<>(authorityRoles());
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role.substring(5) : role;
        return authorityRoles().stream().anyMatch(r -> r.equalsIgnoreCase(target));
    }

    public boolean isAdmin() {
        return hasRole(SecurityRoles.ADMIN);
    }

    public boolean isCashier() {
        return hasRole(SecurityRoles.CASHIER);
    }

    /** Admin or cashier (in-store staff). */
    public boolean isStaff() {
        return isAdmin() || isCashier();
    }

    private Set<String> authorityRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Set.of();
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return Set.of();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());
    }

    public List<String> jwtRealmRoles() {
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
