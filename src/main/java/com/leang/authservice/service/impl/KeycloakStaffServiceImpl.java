package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.exception.ForbiddenException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.CreateStaffUserRequest;
import com.leang.authservice.model.dto.request.UpdateStaffUserRequest;
import com.leang.authservice.model.dto.response.AdminStaffUserResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import com.leang.authservice.service.KeycloakStaffService;
import com.leang.authservice.util.SecurityRoles;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeycloakStaffServiceImpl implements KeycloakStaffService {

    private final Keycloak keycloak;
    private final UserProfileRepository userProfileRepository;

    @Value("${keycloak.realm}")
    private String realmName;

    @Override
    @Transactional
    public AdminStaffUserResponse createStaffUser(CreateStaffUserRequest request) {
        String role = normalizeRole(request.role());
        if (!SecurityRoles.CASHIER.equalsIgnoreCase(role)) {
            throw new BadRequestException("Only role 'cashier' can be created from this endpoint");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setEmailVerified(true);

        try (Response response = keycloak.realm(realmName).users().create(user)) {
            if (response.getStatus() == 409) {
                throw new ConflictException("Username or email already exists", Map.of());
            }
            if (response.getStatus() != 201) {
                throw new IllegalStateException("Keycloak user creation failed: HTTP " + response.getStatus());
            }

            String userId = CreatedResponseUtil.getCreatedId(response);
            boolean temporary = request.temporaryPassword() == null || request.temporaryPassword();
            setPassword(userId, request.password(), temporary);
            assignRealmRole(userId, SecurityRoles.CASHIER);

            UserRepresentation created = keycloak.realm(realmName).users().get(userId).toRepresentation();
            syncUserProfile(created, SecurityRoles.CASHIER.toUpperCase(Locale.ROOT));
            return toResponse(created, SecurityRoles.CASHIER.toUpperCase(Locale.ROOT));
        }
    }

    @Override
    public ApiResponseWithPagination<AdminStaffUserResponse> listStaffUsers(int page, int size, String role, String search) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int first = Math.max(page, 0) * safeSize;

        UsersResource usersResource = keycloak.realm(realmName).users();
        List<UserRepresentation> keycloakUsers;
        if (search != null && !search.isBlank()) {
            keycloakUsers = new ArrayList<>(usersResource.search(search.trim(), first, safeSize));
        } else {
            keycloakUsers = new ArrayList<>(usersResource.list(first, safeSize));
        }

        String roleFilter = role == null || role.isBlank() ? null : role.trim().toLowerCase(Locale.ROOT);

        List<AdminStaffUserResponse> mapped = keycloakUsers.stream()
                .map(u -> {
                    String primaryRole = resolvePrimaryRole(u.getId());
                    return toResponse(u, primaryRole);
                })
                .filter(u -> roleFilter == null || matchesRoleFilter(u.role(), roleFilter))
                .sorted(Comparator.comparing(AdminStaffUserResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return ApiResponseWithPagination.itemsAndPaginationResponse(
                mapped,
                page,
                safeSize,
                mapped.size()
        );
    }

    @Override
    @Transactional
    public AdminStaffUserResponse updateStaffUser(String keycloakUserId, UpdateStaffUserRequest request) {
        UserResource userResource = keycloak.realm(realmName).users().get(keycloakUserId);
        UserRepresentation user = userResource.toRepresentation();
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        String primaryRole = resolvePrimaryRole(keycloakUserId);
        if (SecurityRoles.ADMIN.equalsIgnoreCase(primaryRole)) {
            throw new ForbiddenException("Admin accounts cannot be modified from this endpoint");
        }

        if (request.enabled() != null) {
            if (!request.enabled() && SecurityRoles.ADMIN.equalsIgnoreCase(primaryRole)) {
                throw new ForbiddenException("Cannot disable an admin account");
            }
            user.setEnabled(request.enabled());
            userResource.update(user);
        }

        if (request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            boolean temporary = request.temporaryPasswordFlag() == null || request.temporaryPasswordFlag();
            setPassword(keycloakUserId, request.temporaryPassword(), temporary);
        }

        UserRepresentation updated = userResource.toRepresentation();
        syncUserProfile(updated, primaryRole);
        return toResponse(updated, primaryRole);
    }

    private void setPassword(String userId, String password, boolean temporary) {
        CredentialRepresentation cr = new CredentialRepresentation();
        cr.setType(CredentialRepresentation.PASSWORD);
        cr.setValue(password);
        cr.setTemporary(temporary);
        keycloak.realm(realmName).users().get(userId).resetPassword(cr);
    }

    private void assignRealmRole(String userId, String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(realmName).roles().get(roleName).toRepresentation();
            keycloak.realm(realmName).users().get(userId).roles().realmLevel().add(List.of(role));
        } catch (Exception ex) {
            throw new BadRequestException(
                    "Keycloak realm role '" + roleName + "' is missing. Create it in Keycloak Admin → Realm roles."
            );
        }
    }

    private String resolvePrimaryRole(String userId) {
        List<RoleRepresentation> roles = keycloak.realm(realmName).users().get(userId).roles().realmLevel().listAll();
        if (roles.stream().anyMatch(r -> SecurityRoles.ADMIN.equalsIgnoreCase(r.getName()))) {
            return SecurityRoles.ADMIN.toUpperCase(Locale.ROOT);
        }
        if (roles.stream().anyMatch(r -> SecurityRoles.CASHIER.equalsIgnoreCase(r.getName()))) {
            return SecurityRoles.CASHIER.toUpperCase(Locale.ROOT);
        }
        return SecurityRoles.USER.toUpperCase(Locale.ROOT);
    }

    private void syncUserProfile(UserRepresentation user, String roleLabel) {
        userProfileRepository.findByKeycloakId(user.getId())
                .map(existing -> {
                    existing.setEmail(user.getEmail());
                    existing.setUsername(user.getUsername());
                    existing.setFirstName(user.getFirstName());
                    existing.setLastName(user.getLastName());
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .keycloakId(user.getId())
                                .email(user.getEmail())
                                .username(user.getUsername())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .build()
                ));
    }

    private AdminStaffUserResponse toResponse(UserRepresentation user, String role) {
        Optional<UserProfile> profile = userProfileRepository.findByKeycloakId(user.getId());
        Instant createdAt = profile.map(UserProfile::getCreatedAt).orElse(null);
        if (createdAt == null && user.getCreatedTimestamp() != null) {
            createdAt = Instant.ofEpochMilli(user.getCreatedTimestamp());
        }
        return new AdminStaffUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role,
                Boolean.TRUE.equals(user.isEnabled()),
                createdAt,
                profile.map(UserProfile::getAvatarUrl).orElse(null)
        );
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesRoleFilter(String userRole, String filter) {
        if (userRole == null) {
            return SecurityRoles.USER.equals(filter) || "customer".equals(filter);
        }
        String normalized = userRole.toLowerCase(Locale.ROOT);
        if ("customer".equals(filter)) {
            return SecurityRoles.USER.equals(normalized);
        }
        return normalized.equals(filter);
    }
}
