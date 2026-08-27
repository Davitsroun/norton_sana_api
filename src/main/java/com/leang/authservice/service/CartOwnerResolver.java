package com.leang.authservice.service;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.model.CartOwner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartOwnerResolver {

    private final GuestSessionService guestSessionService;

    /**
     * Prefer JWT user; otherwise ensure a guest session cookie.
     */
    public CartOwner resolve(HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> userId = currentUserId();
        if (userId.isPresent()) {
            return CartOwner.user(userId.get());
        }
        UUID sessionId = guestSessionService.ensureSession(request, response);
        return CartOwner.guest(sessionId);
    }

    /**
     * Guest checkout: must be anonymous with a guest cookie (creates one if missing).
     */
    public CartOwner resolveGuestOnly(HttpServletRequest request, HttpServletResponse response) {
        if (currentUserId().isPresent()) {
            throw new BadRequestException("Guest checkout is for anonymous users. Use the authenticated checkout flow.");
        }
        return CartOwner.guest(guestSessionService.ensureSession(request, response));
    }

    public Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(UUID.fromString(jwtAuth.getToken().getSubject()));
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return Optional.of(UUID.fromString(jwt.getSubject()));
        }
        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Optional<UUID> peekGuestSessionId(HttpServletRequest request) {
        return guestSessionService.findValidSessionId(request);
    }
}
