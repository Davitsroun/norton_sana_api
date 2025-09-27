package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.model.dto.request.AuthRequest;
import com.leang.authservice.model.dto.response.AuthResponse;
import com.leang.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceServiceImpl implements AuthService {
    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realmName;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    @Value("${keycloak.server-url}")
    private String keycloakUrl;
    private final WebClient webClient;

    @SneakyThrows
    @Override
    public AuthResponse login(AuthRequest authorizationRequest) {
        String email = authorizationRequest.email();
        String password = authorizationRequest.password();

        List<UserRepresentation> users = keycloak.realm(realmName)
                .users()
                .searchByEmail(email, true);

        if (users.isEmpty()) {
            throw new BadRequestException("Invalid email or password");
        }

        String username = users.getFirst().getUsername();

        String url = keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/token";
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);  // use username here
        formData.add("password", password);
        formData.add("scope", "openid profile email");

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

            return toAuthResponse(responseBody);

        } catch (WebClientResponseException.Unauthorized ex) {
            throw new BadRequestException("Invalid email or password.");
        }
    }


    @Override
    public void logout(String refreshToken) {
        try {
            String url = keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/logout";
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            System.err.println("Logout failed: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString());
            throw new BadRequestException("Logout failed: " + ex.getResponseBodyAsString());
        }
    }


    @Override
    public AuthResponse refresh(String refreshToken) {
        String url = keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        try {
            Map responseBody = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Refresh failed: empty or malformed response");
            }
            return toAuthResponse(responseBody);
        } catch (WebClientResponseException ex) {
            System.err.println("Refresh failed: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString());
            throw new BadRequestException("Refresh token invalid or expired.");
        }
    }

    private AuthResponse toAuthResponse(Map<String, Object> responseBody) {
        return AuthResponse.builder()
                .token((String) responseBody.get("access_token"))
                .expiresIn((Integer) responseBody.get("expires_in"))
                .refreshExpiresIn((Integer) responseBody.get("refresh_expires_in"))
                .refreshToken((String) responseBody.get("refresh_token"))
                .tokenType((String) responseBody.get("token_type"))
                .idToken((String) responseBody.get("id_token"))
                .notBeforePolicy((Integer) responseBody.get("not-before-policy"))
                .sessionState((String) responseBody.get("session_state"))
                .scope((String) responseBody.get("scope"))
                .build();
    }
}
