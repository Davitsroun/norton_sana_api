package com.leang.authservice.model;

import java.util.UUID;

/**
 * Resolved cart identity: either a logged-in Keycloak user or a guest session.
 */
public record CartOwner(UUID userId, UUID sessionId) {

    public static CartOwner user(UUID userId) {
        return new CartOwner(userId, null);
    }

    public static CartOwner guest(UUID sessionId) {
        return new CartOwner(null, sessionId);
    }

    public boolean isRegistered() {
        return userId != null;
    }

    public boolean isGuest() {
        return userId == null && sessionId != null;
    }
}
