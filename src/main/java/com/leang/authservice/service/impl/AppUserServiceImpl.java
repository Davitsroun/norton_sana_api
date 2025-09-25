package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.AppUserRequest;
import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.request.UpdateAppUserRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.AppUserResponse;
import com.leang.authservice.model.dto.response.AuthResponse;
import com.leang.authservice.service.AppUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {
    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realmName;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    @Value("${keycloak.server-url}")
    private String keycloakUrl;
    private final WebClient webClient = WebClient.create();

    @Override
    public AppUserResponse createUser(AppUserRequest req) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

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
    public ApiResponseWithPagination<AppUserResponse> findAllUsers(String username, String email, Integer page, Integer size) {
        int offset = (page - 1) * size;

        try {
            // user is provided
            if (username != null && !username.isBlank()) {
                var serverSide = keycloak.realm(realmName).users().search(username, offset, size);
                //email is provided too
                if (email != null && !email.isBlank()) {
                    var allMatches = keycloak.realm(realmName).users().search(username);
                    var filtered = allMatches.stream()
                            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                            .toList();
                    var pageSlice = slice(filtered, offset, size);
                    return buildResponse(pageSlice.stream().map(this::toAppUserResponse).toList(), page, size, filtered.size());
                } else {
                    long totalUsers = serverSide.size();
                    var pageItems = serverSide.stream().map(this::toAppUserResponse).toList();
                    return buildResponse(pageItems, page, size, (int) totalUsers);
                }
            }

            //if only email is provided
            if (email != null && !email.isBlank()) {
                var matches = keycloak.realm(realmName).users().searchByEmail(email, true);
                long total = matches.size();
                var pageSlice = slice(matches, offset, size);
                var pageItems = pageSlice.stream().map(this::toAppUserResponse).toList();
                return buildResponse(pageItems, page, size, (int) total);
            }

            //no filter apply
            var usersPage = keycloak.realm(realmName).users().list(offset, size);
            var totalUsers = usersPage.size();
            var pageSlice = slice(usersPage, offset, size);
            var pageItems = pageSlice.stream().map(this::toAppUserResponse).toList();
            return buildResponse(pageItems, page, size, totalUsers);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch users", ex);
        }
    }

    @Override
    public AppUserResponse getUserById(String userId) {
        UserRepresentation representation = keycloak.realm(realmName).users().get(userId).toRepresentation();
        if (representation == null) {
            throw new NotFoundException("User with id '" + userId + "' not found.");
        }
        return toAppUserResponse(representation);

    }

    @SneakyThrows
    @Override
    public AuthResponse login(AuthRequest authorizationRequest) {
        String url = keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", authorizationRequest.username());
        formData.add("password", authorizationRequest.password());

        try {
            Map responseBody = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Login failed: empty or malformed response from Keycloak");
            }

            return new AuthResponse((String) responseBody.get("access_token"));

        } catch (WebClientResponseException.Unauthorized ex) {
            System.err.println("Login failed: Invalid username or password.");
            System.err.println("Error body: " + ex.getResponseBodyAsString());
            throw new BadRequestException("Invalid username or password.");

        } catch (WebClientResponseException ex) {
            System.err.println("WebClient error during login: " + ex.getStatusCode() + " " + ex.getStatusText());
            System.err.println("Error body: " + ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to connect to Keycloak server.");
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

        return new AppUserResponse(userId, username, email, firstName, lastName);
    }

    @Override
    public AppUserResponse updateCurrentUserProfile(UpdateAppUserRequest dto) {
        AppUserResponse current = getUserProfile();
        String userId = current.getId();

        UsersResource usersResource = keycloak.realm(realmName).users();
        UserResource userResource = usersResource.get(userId);
        UserRepresentation user = userResource.toRepresentation();

        if (user == null) {
            throw new BadRequestException("Invalid token or user not found");
        }

        String newUsername = safeTrim(dto.username());
        String newEmail = safeTrim(dto.email());

        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            List<UserRepresentation> found = usersResource.search(newUsername);
            boolean conflict = found.stream().anyMatch(u -> !Objects.equals(u.getId(), userId)
                    && newUsername.equalsIgnoreCase(u.getUsername()));
            if (conflict) {
                throw new BadRequestException("Username already in use");
            }
            user.setUsername(newUsername);
        }

        if (newEmail != null && !newEmail.isBlank() && !Objects.equals(newEmail, user.getEmail())) {
            List<UserRepresentation> foundByEmail = usersResource.search(newEmail); // search finds by email too
            boolean conflict = foundByEmail.stream().anyMatch(u -> !Objects.equals(u.getId(), userId)
                    && newEmail.equalsIgnoreCase(u.getEmail()));
            if (conflict) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(newEmail);
        }

        if (dto.firstName() != null) user.setFirstName(newUsername);
        if (dto.lastName() != null) user.setLastName(newEmail);

        try {
            userResource.update(user);
        } catch (WebApplicationException ex) {
            String body = "(empty)";
            try {
                body = ex.getResponse().readEntity(String.class);
            } catch (Exception e) {
                // ignore reading error
            }
            // convert to meaningful exception for client
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
    public void deleteCurrentUserProfile() {
        var usersResource = keycloak.realm(realmName).users();
        AppUserResponse userProfile = getUserProfile();
        //validation
        getUserById(userProfile.getId());
        usersResource.delete(userProfile.getId());
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private String safeReadResponseBody(WebApplicationException ex) {
        try {
            return ex.getResponse().readEntity(String.class);
        } catch (Exception ignored) {
            return "(unreadable)";
        }
    }

    private ApiResponseWithPagination<AppUserResponse> buildResponse(
            List<AppUserResponse> items, int page, int size, int total) {
        // Try to use a constructor you have; adapt the call below to your ApiResponseWithPagination signature.
        return ApiResponseWithPagination.itemsAndPaginationResponse(items, page, size, total);
    }

    private <T> List<T> slice(List<T> list, int offset, Integer size) {
        if (list.isEmpty() || offset >= list.size()) return Collections.emptyList();
        int toIndex = Math.min(list.size(), offset + size);
        return list.subList(offset, toIndex);
    }

    private AppUserResponse toAppUserResponse(UserRepresentation userRepresentation) {
        return new AppUserResponse(
                userRepresentation.getId(),
                userRepresentation.getUsername(),
                userRepresentation.getEmail(),
                userRepresentation.getFirstName(),
                userRepresentation.getLastName()
        );
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

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
