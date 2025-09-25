package com.leang.authservice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProps(
        String serverUrl, String realm, String clientId, String clientSecret,
        Long connectionTimeoutMs, Long readTimeoutMs, String username, String password) {
}

