package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.request.UpdatePasswordRequest;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.service.AppUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {
    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realmName;

    @Override
    public AppUserResponse createUser(AppUserRequest req) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setAttributes(Map.of("imageUrl", List.of(req.imageUrl())));

        try (Response response = keycloak.realm(realmName).users().create(user)) {
            int status = response.getStatus();
            String body = safeReadBody(response);

            if (status == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                setPasswordIfProvided(userId, req.password());

                UserRepresentation createdUser = keycloak.realm(realmName).users()
                        .get(userId).toRepresentation();
                return toAppUserResponse(createdUser);
            }

            if (status == 409) {
                throw new ConflictException(
                        "Duplicate value for one or more fields.",
                        detectConflicts(req)
                );
            }

            throw new IllegalStateException("User creation failed: HTTP " + status +
                    (body.isBlank() ? "" : " – " + body));
        }
    }



    @Override
    public AppUserResponse getUserProfile() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // extract info from token
        String userId = jwt.getClaimAsString("sub");
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        return AppUserResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    @Override
    public AppUserResponse updateCurrentUserProfile(UpdateAppUserRequest dto) {
        AppUserResponse current = getUserProfile();
        String userId = current.getUserId();

        UsersResource usersResource = keycloak.realm(realmName).users();
        UserResource userResource = usersResource.get(userId);
        UserRepresentation user = userResource.toRepresentation();

        if (user == null) {
            throw new BadRequestException("Invalid token or user not found");
        }

        String newUsername = safeTrim(dto.username());

        if (!newUsername.equals(user.getUsername())) {
            List<UserRepresentation> found = usersResource.search(newUsername);
            boolean conflict = found.stream().anyMatch(u -> !Objects.equals(u.getId(), userId)
                    && newUsername.equalsIgnoreCase(u.getUsername()));
            if (conflict) {
                throw new BadRequestException("Username already in use");
            }
            user.setUsername(newUsername);
        }

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        if (dto.imageUrl() != null) {
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) attributes = new HashMap<>();
            attributes.put("imageUrl", List.of(dto.imageUrl()));
            user.setAttributes(attributes);
        }

        try {
            userResource.update(user);
        } catch (WebApplicationException ex) {
            String body;
            try {
                body = ex.getResponse().readEntity(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (ex.getResponse().getStatus() == 409) {
                throw new BadRequestException("Conflict updating user: " + body);
            } else {
                throw new BadRequestException("Failed to update user: " + body);
            }
        }
        UserRepresentation updated = userResource.toRepresentation();
        return toAppUserResponse(updated);
    }


    @Override
    public void updateUserPassword(UpdatePasswordRequest updatePasswordRequest) {
        AppUserResponse current = getUserProfile();
        String userId = current.getUserId();

        UsersResource usersResource = keycloak.realm(realmName).users();
        UserResource userResource = usersResource.get(userId);
        UserRepresentation user = userResource.toRepresentation();

        if (user == null) {
            throw new BadRequestException("User not found or invalid token");
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(updatePasswordRequest.getNewPassword().trim());
        credential.setTemporary(false);
        try {
            userResource.resetPassword(credential);
        } catch (WebApplicationException ex) {
            String body = "(empty)";
            try {
                body = ex.getResponse().readEntity(String.class);
            } catch (Exception ignored) {
            }
            throw new BadRequestException("Failed to update password: " + body);
        }
    }


    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }


    private AppUserResponse toAppUserResponse(UserRepresentation userRepresentation) {
        String imageUrl = null;
        if (userRepresentation.getAttributes() != null) {
            List<String> values = userRepresentation.getAttributes().get("imageUrl");
            if (values != null && !values.isEmpty()) {
                imageUrl = values.getFirst();
            }
        }
        return AppUserResponse.builder()
                .userId(userRepresentation.getId())
                .username(userRepresentation.getUsername())
                .email(userRepresentation.getEmail())
                .firstName(userRepresentation.getFirstName())
                .lastName(userRepresentation.getLastName())
                .imageUrl(imageUrl)
                .build();
    }

    private void setPasswordIfProvided(String userId, String password) {
        if (password == null || password.isBlank()) return;
        CredentialRepresentation cr = new CredentialRepresentation();
        cr.setType(CredentialRepresentation.PASSWORD);
        cr.setValue(password);
        cr.setTemporary(false);
        keycloak.realm(realmName).users().get(userId).resetPassword(cr);
    }

    private Map<String, String> detectConflicts(AppUserRequest req) {
        Map<String, String> conflicts = new HashMap<>();
        try {
            if (!keycloak.realm(realmName).users().searchByUsername(req.username(), true).isEmpty()) {
                conflicts.put("username", "Username is already taken.");
            }
            if (!keycloak.realm(realmName).users().searchByEmail(req.email(), true).isEmpty()) {
                conflicts.put("email", "Email has already been used.");
            }
        } catch (Throwable ignored) {
        }
        return conflicts;
    }

    private String safeReadBody(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            return "";
        }
    }


}
