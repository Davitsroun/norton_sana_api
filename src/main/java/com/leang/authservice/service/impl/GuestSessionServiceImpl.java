package com.leang.authservice.service.impl;

import com.leang.authservice.model.entity.GuestSession;
import com.leang.authservice.repository.GuestSessionRepository;
import com.leang.authservice.service.GuestSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestSessionServiceImpl implements GuestSessionService {

    private final GuestSessionRepository guestSessionRepository;

    @Override
    @Transactional
    public UUID ensureSession(HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> existing = findValidSessionId(request);
        if (existing.isPresent()) {
            UUID id = existing.get();
            guestSessionRepository.findById(id).ifPresent(session -> {
                session.setLastSeenAt(Instant.now());
                guestSessionRepository.save(session);
            });
            writeCookie(response, id, COOKIE_MAX_AGE_SECONDS);
            return id;
        }
        Instant now = Instant.now();
        GuestSession session = GuestSession.builder()
                .id(UUID.randomUUID())
                .createdAt(now)
                .lastSeenAt(now)
                .expiresAt(now.plusSeconds(COOKIE_MAX_AGE_SECONDS))
                .build();
        guestSessionRepository.save(session);
        writeCookie(response, session.getId(), COOKIE_MAX_AGE_SECONDS);
        return session.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findValidSessionId(HttpServletRequest request) {
        String raw = readCookie(request);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        UUID id;
        try {
            id = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        return guestSessionRepository.findById(id)
                .filter(session -> session.getExpiresAt() != null && session.getExpiresAt().isAfter(Instant.now()))
                .map(GuestSession::getId);
    }

    @Override
    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, UUID sessionId, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, sessionId.toString())
                .path("/")
                .maxAge(Duration.ofSeconds(Math.max(maxAgeSeconds, 0)))
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
