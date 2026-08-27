package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.GuestSessionResponse;
import com.leang.authservice.service.GuestSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guest")
@RequiredArgsConstructor
@Tag(name = "GuestSession")
public class GuestSessionController extends BaseResponse {

    private final GuestSessionService guestSessionService;

    @Operation(summary = "Ensure guest session cookie and return session id")
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<GuestSessionResponse>> ensureSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UUID sessionId = guestSessionService.ensureSession(request, response);
        return responseEntity(true, "Guest session ready.", HttpStatus.OK, new GuestSessionResponse(sessionId));
    }
}
