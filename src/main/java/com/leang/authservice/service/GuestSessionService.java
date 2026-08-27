package com.leang.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import java.util.UUID;

public interface GuestSessionService {

    String COOKIE_NAME = "GUEST_SESSION_ID";
    int COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

    /**
     * Read cookie or create a new guest session and set {@code GUEST_SESSION_ID}.
     */
    UUID ensureSession(HttpServletRequest request, HttpServletResponse response);

    /**
     * Valid session from cookie only (does not create).
     */
    Optional<UUID> findValidSessionId(HttpServletRequest request);

    /**
     * Expire the guest cookie on the response (e.g. after merge).
     */
    void clearSessionCookie(HttpServletResponse response);
}
