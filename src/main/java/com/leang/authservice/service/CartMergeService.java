package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.OrderViewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface CartMergeService {

    /**
     * Merge guest-session pending cart into the authenticated user's pending cart.
     */
    OrderViewResponse mergeGuestCartIntoUser(HttpServletRequest request, HttpServletResponse response);

    /**
     * Idempotent merge when JWT user is present and a guest session cookie/header exists.
     */
    void mergeGuestCartIfPresent(UUID userId, HttpServletRequest request, HttpServletResponse response);
}
